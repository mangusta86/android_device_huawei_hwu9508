package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaDisplayInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaLineControlInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaNumberInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaRedirectingNumberInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaSignalInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaT53AudioControlInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaT53ClirInfoRec;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.cdma.DataProfileOmh;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.gsm.SsData.RequestType;
import com.android.internal.telephony.gsm.SsData.ServiceType;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RIL extends BaseCommands
  implements CommandsInterface
{
  private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;
  private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
  private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 60000;
  static final int EVENT_SEND = 1;
  static final int EVENT_WAKE_LOCK_TIMEOUT = 2;
  static final String LOG_TAG = "RILJ";
  static final int RESPONSE_SOLICITED = 0;
  static final int RESPONSE_UNSOLICITED = 1;
  static final boolean RILJ_LOGD = true;
  static final boolean RILJ_LOGV = false;
  static final int RIL_MAX_COMMAND_BYTES = 8192;
  static final String SOCKET_NAME_RIL = "rild";
  static final String SOCKET_NAME_RIL1 = "rild1";
  static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
  private Integer mInstanceId;
  BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramAnonymousContext, Intent paramAnonymousIntent)
    {
      if (paramAnonymousIntent.getAction().equals("android.intent.action.SCREEN_ON"))
      {
        RIL.this.sendScreenState(true);
        return;
      }
      if (paramAnonymousIntent.getAction().equals("android.intent.action.SCREEN_OFF"))
      {
        RIL.this.sendScreenState(false);
        return;
      }
      Log.w("RILJ", "RIL received unexpected Intent: " + paramAnonymousIntent.getAction());
    }
  };
  Object mLastNITZTimeInfo;
  private RILEx mRILEx = new RILEx(null);
  RILReceiver mReceiver;
  Thread mReceiverThread;
  int mRequestMessagesPending;
  int mRequestMessagesWaiting;
  ArrayList<RILRequest> mRequestsList = new ArrayList();
  RILSender mSender;
  HandlerThread mSenderThread;
  private int mSetPreferredNetworkType;
  LocalSocket mSocket;
  AtomicBoolean mTestingEmergencyCall = new AtomicBoolean(false);
  PowerManager.WakeLock mWakeLock;
  int mWakeLockTimeout;

  public RIL(Context paramContext, int paramInt1, int paramInt2)
  {
    this(paramContext, paramInt1, paramInt2, null);
  }

  public RIL(Context paramContext, int paramInt1, int paramInt2, Integer paramInteger)
  {
    super(paramContext);
    riljLog("RIL(context, preferredNetworkType=" + paramInt1 + " cdmaSubscription=" + paramInt2 + ")");
    this.mCdmaSubscription = paramInt2;
    this.mPreferredNetworkType = paramInt1;
    this.mPhoneType = 0;
    this.mInstanceId = paramInteger;
    this.mWakeLock = ((PowerManager)paramContext.getSystemService("power")).newWakeLock(1, "RILJ");
    this.mWakeLock.setReferenceCounted(false);
    this.mWakeLockTimeout = SystemProperties.getInt("ro.ril.wake_lock_timeout", 60000);
    this.mRequestMessagesPending = 0;
    this.mRequestMessagesWaiting = 0;
    this.mSenderThread = new HandlerThread("RILSender");
    this.mSenderThread.start();
    this.mSender = new RILSender(this.mSenderThread.getLooper());
    if (!((ConnectivityManager)paramContext.getSystemService("connectivity")).isNetworkSupported(0))
    {
      riljLog("Not starting RILReceiver: wifi-only");
      return;
    }
    riljLog("Starting RILReceiver");
    this.mReceiver = new RILReceiver();
    this.mReceiverThread = new Thread(this.mReceiver, "RILReceiver");
    this.mReceiverThread.start();
    IntentFilter localIntentFilter = new IntentFilter();
    localIntentFilter.addAction("android.intent.action.SCREEN_ON");
    localIntentFilter.addAction("android.intent.action.SCREEN_OFF");
    paramContext.registerReceiver(this.mIntentReceiver, localIntentFilter);
  }

  private void acquireWakeLock()
  {
    synchronized (this.mWakeLock)
    {
      this.mWakeLock.acquire();
      this.mRequestMessagesPending = (1 + this.mRequestMessagesPending);
      this.mSender.removeMessages(2);
      Message localMessage = this.mSender.obtainMessage(2);
      this.mSender.sendMessageDelayed(localMessage, this.mWakeLockTimeout);
      return;
    }
  }

  private void clearRequestsList(int paramInt, boolean paramBoolean)
  {
    while (true)
    {
      synchronized (this.mRequestsList)
      {
        int i = this.mRequestsList.size();
        if (paramBoolean)
        {
          Log.d("RILJ", "WAKE_LOCK_TIMEOUT  mReqPending=" + this.mRequestMessagesPending + " mRequestList=" + i);
          break label180;
          if (j < i)
          {
            RILRequest localRILRequest = (RILRequest)this.mRequestsList.get(j);
            if (paramBoolean)
              Log.d("RILJ", j + ": [" + localRILRequest.mSerial + "] " + requestToString(localRILRequest.mRequest));
            localRILRequest.onError(paramInt, null);
            localRILRequest.release();
            j++;
            continue;
          }
          this.mRequestsList.clear();
          this.mRequestMessagesWaiting = 0;
          return;
        }
      }
      label180: int j = 0;
    }
  }

  private void constructCdmaSendSmsRilRequest(RILRequest paramRILRequest, byte[] paramArrayOfByte)
  {
    DataInputStream localDataInputStream = new DataInputStream(new ByteArrayInputStream(paramArrayOfByte));
    try
    {
      paramRILRequest.mp.writeInt(localDataInputStream.readInt());
      paramRILRequest.mp.writeByte((byte)localDataInputStream.readInt());
      paramRILRequest.mp.writeInt(localDataInputStream.readInt());
      paramRILRequest.mp.writeInt(localDataInputStream.read());
      paramRILRequest.mp.writeInt(localDataInputStream.read());
      paramRILRequest.mp.writeInt(localDataInputStream.read());
      paramRILRequest.mp.writeInt(localDataInputStream.read());
      byte b1 = (byte)localDataInputStream.read();
      paramRILRequest.mp.writeByte(b1);
      for (byte b2 = 0; b2 < b1; b2++)
        paramRILRequest.mp.writeByte(localDataInputStream.readByte());
      paramRILRequest.mp.writeInt(localDataInputStream.read());
      paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
      byte b3 = (byte)localDataInputStream.read();
      paramRILRequest.mp.writeByte(b3);
      for (byte b4 = 0; b4 < b3; b4++)
        paramRILRequest.mp.writeByte(localDataInputStream.readByte());
      int i = localDataInputStream.read();
      paramRILRequest.mp.writeInt(i);
      for (int j = 0; j < i; j++)
        paramRILRequest.mp.writeByte(localDataInputStream.readByte());
    }
    catch (IOException localIOException)
    {
      riljLog("sendSmsCdma: conversion from input stream to object failed: " + localIOException);
    }
  }

  private void constructGsmSendSmsRilRequest(RILRequest paramRILRequest, String paramString1, String paramString2)
  {
    paramRILRequest.mp.writeInt(2);
    paramRILRequest.mp.writeString(paramString1);
    paramRILRequest.mp.writeString(paramString2);
  }

  private RILRequest findAndRemoveRequestFromList(int paramInt)
  {
    ArrayList localArrayList = this.mRequestsList;
    for (int i = 0; ; i++)
      try
      {
        int j = this.mRequestsList.size();
        if (i < j)
        {
          RILRequest localRILRequest = (RILRequest)this.mRequestsList.get(i);
          if (localRILRequest.mSerial == paramInt)
          {
            this.mRequestsList.remove(i);
            if (this.mRequestMessagesWaiting > 0)
              this.mRequestMessagesWaiting = (-1 + this.mRequestMessagesWaiting);
            return localRILRequest;
          }
        }
        else
        {
          return null;
        }
      }
      finally
      {
      }
  }

  private DataCallState getDataCallState(Parcel paramParcel, int paramInt)
  {
    DataCallState localDataCallState = new DataCallState();
    localDataCallState.version = paramInt;
    if (paramInt < 5)
    {
      localDataCallState.cid = paramParcel.readInt();
      localDataCallState.active = paramParcel.readInt();
      localDataCallState.type = paramParcel.readString();
      String str4 = paramParcel.readString();
      if (!TextUtils.isEmpty(str4))
        localDataCallState.addresses = str4.split(" ");
    }
    String str3;
    do
    {
      return localDataCallState;
      localDataCallState.status = paramParcel.readInt();
      localDataCallState.suggestedRetryTime = paramParcel.readInt();
      localDataCallState.cid = paramParcel.readInt();
      localDataCallState.active = paramParcel.readInt();
      localDataCallState.type = paramParcel.readString();
      localDataCallState.ifname = paramParcel.readString();
      if ((localDataCallState.status == DataConnection.FailCause.NONE.getErrorCode()) && (TextUtils.isEmpty(localDataCallState.ifname)) && (localDataCallState.active != 0))
        throw new RuntimeException("getDataCallState, no ifname");
      String str1 = paramParcel.readString();
      if (!TextUtils.isEmpty(str1))
        localDataCallState.addresses = str1.split(" ");
      String str2 = paramParcel.readString();
      if (!TextUtils.isEmpty(str2))
        localDataCallState.dnses = str2.split(" ");
      str3 = paramParcel.readString();
    }
    while (TextUtils.isEmpty(str3));
    localDataCallState.gateways = str3.split(" ");
    return localDataCallState;
  }

  private CommandsInterface.RadioState getRadioStateFromInt(int paramInt)
  {
    switch (paramInt)
    {
    default:
      throw new RuntimeException("Unrecognized RIL_RadioState: " + paramInt);
    case 0:
      return CommandsInterface.RadioState.RADIO_OFF;
    case 1:
      return CommandsInterface.RadioState.RADIO_UNAVAILABLE;
    case 2:
      return CommandsInterface.RadioState.SIM_NOT_READY;
    case 3:
      return CommandsInterface.RadioState.SIM_LOCKED_OR_ABSENT;
    case 4:
      return CommandsInterface.RadioState.SIM_READY;
    case 5:
      return CommandsInterface.RadioState.RUIM_NOT_READY;
    case 6:
      return CommandsInterface.RadioState.RUIM_READY;
    case 7:
      return CommandsInterface.RadioState.RUIM_LOCKED_OR_ABSENT;
    case 8:
      return CommandsInterface.RadioState.NV_NOT_READY;
    case 9:
      return CommandsInterface.RadioState.NV_READY;
    case 10:
      return CommandsInterface.RadioState.RADIO_ON;
    case 11:
    }
    return CommandsInterface.RadioState.SIM_LOCKED_OR_ABSENT;
  }

  private void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords paramCdmaInformationRecords)
  {
    if ((paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaDisplayInfoRec))
      if (this.mDisplayInfoRegistrants != null)
      {
        unsljLogRet(1027, paramCdmaInformationRecords.record);
        this.mDisplayInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
      }
    do
    {
      do
      {
        do
        {
          do
          {
            do
            {
              do
              {
                return;
                if (!(paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaSignalInfoRec))
                  break;
              }
              while (this.mSignalInfoRegistrants == null);
              unsljLogRet(1027, paramCdmaInformationRecords.record);
              this.mSignalInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
              return;
              if (!(paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaNumberInfoRec))
                break;
            }
            while (this.mNumberInfoRegistrants == null);
            unsljLogRet(1027, paramCdmaInformationRecords.record);
            this.mNumberInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
            return;
            if (!(paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec))
              break;
          }
          while (this.mRedirNumInfoRegistrants == null);
          unsljLogRet(1027, paramCdmaInformationRecords.record);
          this.mRedirNumInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
          return;
          if (!(paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec))
            break;
        }
        while (this.mLineControlInfoRegistrants == null);
        unsljLogRet(1027, paramCdmaInformationRecords.record);
        this.mLineControlInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
        return;
        if (!(paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec))
          break;
      }
      while (this.mT53ClirInfoRegistrants == null);
      unsljLogRet(1027, paramCdmaInformationRecords.record);
      this.mT53ClirInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
      return;
    }
    while ((!(paramCdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec)) || (this.mT53AudCntrlInfoRegistrants == null));
    unsljLogRet(1027, paramCdmaInformationRecords.record);
    this.mT53AudCntrlInfoRegistrants.notifyRegistrants(new AsyncResult(null, paramCdmaInformationRecords.record, null));
  }

  private void notifyRegistrantsRilConnectionChanged(int paramInt)
  {
    this.mRilVersion = paramInt;
    if (this.mRilConnectedRegistrants != null)
      this.mRilConnectedRegistrants.notifyRegistrants(new AsyncResult(null, new Integer(paramInt), null));
  }

  private void processResponse(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    if (i == 1)
      processUnsolicited(paramParcel);
    while (true)
    {
      releaseWakeLockIfDone();
      return;
      if (i == 0)
        processSolicited(paramParcel);
    }
  }

  private void processSolicited(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    int j = paramParcel.readInt();
    RILRequest localRILRequest = findAndRemoveRequestFromList(i);
    if (localRILRequest == null)
    {
      Log.w("RILJ", "Unexpected solicited response! sn: " + i + " error: " + j);
      return;
    }
    Object localObject2;
    if (j != 0)
    {
      int k = paramParcel.dataAvail();
      localObject2 = null;
      if (k <= 0);
    }
    else
    {
      try
      {
        switch (localRILRequest.mRequest)
        {
        default:
          throw new RuntimeException("Unrecognized solicited response: " + localRILRequest.mRequest);
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
        case 13:
        case 14:
        case 15:
        case 16:
        case 17:
        case 18:
        case 19:
        case 20:
        case 21:
        case 22:
        case 23:
        case 24:
        case 25:
        case 513:
        case 515:
        case 514:
        case 516:
        case 517:
        case 518:
        case 26:
        case 27:
        case 28:
        case 523:
        case 524:
        case 525:
        case 526:
        case 29:
        case 30:
        case 31:
        case 32:
        case 33:
        case 34:
        case 35:
        case 36:
        case 37:
        case 38:
        case 39:
        case 40:
        case 41:
        case 42:
        case 43:
        case 44:
        case 45:
        case 46:
        case 47:
        case 48:
        case 49:
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
        case 55:
        case 56:
        case 57:
        case 58:
        case 59:
        case 60:
        case 61:
        case 62:
        case 63:
        case 64:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 71:
        case 72:
        case 73:
        case 74:
        case 75:
        case 76:
        case 77:
        case 78:
        case 79:
        case 80:
        case 81:
        case 82:
        case 83:
        case 84:
        case 85:
        case 87:
        case 88:
        case 89:
        case 90:
        case 91:
        case 92:
        case 93:
        case 94:
        case 86:
        case 95:
        case 96:
        case 97:
        case 98:
        case 100:
        case 101:
        case 99:
        case 102:
        case 103:
        case 104:
        case 506:
        case 105:
        case 108:
        case 504:
        case 505:
        case 512:
        case 507:
        case 508:
        case 509:
        case 510:
        case 511:
        case 106:
        case 107:
        case 503:
        case 501:
        case 502:
        case 519:
        case 520:
        case 521:
        case 522:
        case 536:
        }
      }
      catch (Throwable localThrowable)
      {
        Log.w("RILJ", localRILRequest.serialString() + "< " + requestToString(localRILRequest.mRequest) + " exception, possible invalid RIL response", localThrowable);
        if (localRILRequest.mResult != null)
        {
          AsyncResult.forMessage(localRILRequest.mResult, null, localThrowable);
          localRILRequest.mResult.sendToTarget();
        }
        localRILRequest.release();
        return;
      }
      Object localObject3 = responseIccCardStatus(paramParcel);
      localObject2 = localObject3;
    }
    switch (localRILRequest.mRequest)
    {
    case 4:
    default:
      if (j != 0)
        switch (localRILRequest.mRequest)
        {
        default:
        case 2:
        case 4:
        case 6:
        case 7:
        case 43:
        }
      break;
    case 3:
    case 5:
      while (true)
      {
        label1332: localRILRequest.onError(j, localObject2);
        localRILRequest.release();
        return;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseCallList(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        if ((this.mTestingEmergencyCall.getAndSet(false)) && (this.mEmergencyCallbackModeRegistrant != null))
        {
          riljLog("testing emergency call, notify ECM Registrants");
          this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
        }
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseSignalStrength(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseSMS(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseSMS(paramParcel);
        break;
        localObject2 = responseSetupDataCall(paramParcel);
        break;
        localObject2 = responseICC_IO(paramParcel);
        break;
        localObject2 = responseICC_IO(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseICC_IO(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseCallForward(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseOperatorInfos(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseDataCallList(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseRaw(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseGetPreferredNetworkType(paramParcel);
        break;
        localObject2 = responseCellList(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseSMS(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseGmsBroadcastConfig(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseCdmaBroadcastConfig(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseStrings(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseGetDataCallProfile(paramParcel);
        break;
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseSMS(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseUiccSubscription(paramParcel);
        break;
        localObject2 = responseInts(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseICC_IO(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        Object localObject1 = responseVoid(paramParcel);
        localObject2 = localObject1;
        break;
        if (this.mIccStatusChangedRegistrants == null)
          break label1332;
        riljLog("ON enter sim puk fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
        this.mIccStatusChangedRegistrants.notifyRegistrants();
        break label1332;
        if (this.mIccStatusChangedRegistrants != null)
        {
          riljLog("ON some errors fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
          this.mIccStatusChangedRegistrants.notifyRegistrants();
        }
      }
    }
    riljLog(localRILRequest.serialString() + "< " + requestToString(localRILRequest.mRequest) + " " + retToString(localRILRequest.mRequest, localObject2));
    if (localRILRequest.mResult != null)
    {
      AsyncResult.forMessage(localRILRequest.mResult, localObject2, null);
      localRILRequest.mResult.sendToTarget();
    }
    localRILRequest.release();
  }

  private void processUnsolicited(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    switch (i)
    {
    default:
    case 1000:
    case 1001:
    case 1002:
    case 1505:
    case 1003:
    case 1004:
    case 1005:
    case 1006:
    case 1008:
    case 1009:
    case 1010:
    case 1011:
    case 1012:
    case 1013:
    case 1014:
    case 1015:
    case 1016:
    case 1017:
    case 1018:
    case 1023:
    case 1019:
    case 1020:
    case 1021:
    case 1022:
    case 1024:
    case 1025:
    case 1026:
    case 1027:
    case 1028:
    case 1029:
    case 1030:
    case 1031:
    case 1032:
    case 1033:
    case 1034:
    case 1501:
    case 1035:
    case 1503:
    case 1504:
    case 1506:
    case 1507:
    case 1508:
    case 1509:
    case 1510:
    case 1502:
    }
    Object localObject2;
    do
    {
      do
      {
        do
        {
          do
          {
            do
            {
              do
              {
                do
                {
                  do
                  {
                    do
                    {
                      do
                      {
                        do
                        {
                          do
                          {
                            do
                            {
                              while (true)
                              {
                                try
                                {
                                  throw new RuntimeException("Unrecognized unsol response: " + i);
                                }
                                catch (Throwable localThrowable)
                                {
                                  Log.e("RILJ", "Exception processing unsol response: " + i + "Exception:" + localThrowable.toString());
                                  return;
                                }
                                Object localObject3 = responseVoid(paramParcel);
                                localObject2 = localObject3;
                                while (true)
                                  switch (i)
                                  {
                                  default:
                                    return;
                                  case 1000:
                                    CommandsInterface.RadioState localRadioState = getRadioStateFromInt(paramParcel.readInt());
                                    unsljLogMore(i, localRadioState.toString());
                                    switchToRadioState(localRadioState);
                                    return;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseString(paramParcel);
                                    continue;
                                    localObject2 = responseString(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseStrings(paramParcel);
                                    continue;
                                    localObject2 = responseString(paramParcel);
                                    continue;
                                    localObject2 = responseSignalStrength(paramParcel);
                                    continue;
                                    localObject2 = responseDataCallList(paramParcel);
                                    continue;
                                    localObject2 = responseSuppServiceNotification(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseString(paramParcel);
                                    continue;
                                    localObject2 = responseString(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseSimRefresh(paramParcel);
                                    continue;
                                    localObject2 = responseCallRing(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseCdmaSms(paramParcel);
                                    continue;
                                    localObject2 = responseRaw(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseCdmaCallWaiting(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseCdmaInformationRecord(paramParcel);
                                    continue;
                                    localObject2 = responseRaw(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseVoid(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseSSData(paramParcel);
                                    continue;
                                    localObject2 = responseString(paramParcel);
                                    continue;
                                    localObject2 = responseInts(paramParcel);
                                    continue;
                                    localObject2 = responseStrings(paramParcel);
                                    continue;
                                    localObject2 = responseModifyCall(paramParcel);
                                    continue;
                                    Object localObject1 = responseInts(paramParcel);
                                    localObject2 = localObject1;
                                  case 1503:
                                  case 1001:
                                  case 1002:
                                  case 1505:
                                  case 1003:
                                  case 1004:
                                  case 1005:
                                  case 1006:
                                  case 1008:
                                  case 1009:
                                  case 1010:
                                  case 1011:
                                  case 1012:
                                  case 1013:
                                  case 1014:
                                  case 1015:
                                  case 1016:
                                  case 1017:
                                  case 1018:
                                  case 1023:
                                  case 1019:
                                  case 1020:
                                  case 1021:
                                  case 1022:
                                  case 1024:
                                  case 1025:
                                  case 1026:
                                  case 1027:
                                  case 1028:
                                  case 1029:
                                  case 1030:
                                  case 1035:
                                  case 1031:
                                  case 1506:
                                  case 1507:
                                  case 1032:
                                  case 1033:
                                  case 1510:
                                  case 1034:
                                  case 1504:
                                  case 1508:
                                  case 1509:
                                  case 1501:
                                  case 1502:
                                  }
                                unsljLog(i);
                                this.mImsNetworkStateChangedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                                return;
                                unsljLog(i);
                                this.mCallStateRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                                return;
                                unsljLog(i);
                                this.mVoiceNetworkStateRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                                return;
                                unsljLog(i);
                                this.mDataNetworkStateRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                                return;
                                unsljLog(i);
                                String[] arrayOfString2 = new String[2];
                                arrayOfString2[1] = ((String)localObject2);
                                SmsMessage localSmsMessage2 = SmsMessage.newFromCMT(arrayOfString2);
                                if (this.mGsmSmsRegistrant != null)
                                {
                                  Registrant localRegistrant18 = this.mGsmSmsRegistrant;
                                  AsyncResult localAsyncResult30 = new AsyncResult(null, localSmsMessage2, null);
                                  localRegistrant18.notifyRegistrant(localAsyncResult30);
                                  return;
                                  unsljLogRet(i, localObject2);
                                  if (this.mSmsStatusRegistrant != null)
                                  {
                                    Registrant localRegistrant17 = this.mSmsStatusRegistrant;
                                    AsyncResult localAsyncResult29 = new AsyncResult(null, localObject2, null);
                                    localRegistrant17.notifyRegistrant(localAsyncResult29);
                                    return;
                                    unsljLogRet(i, localObject2);
                                    int[] arrayOfInt = (int[])localObject2;
                                    if (arrayOfInt.length == 1)
                                    {
                                      if (this.mSmsOnSimRegistrant != null)
                                      {
                                        Registrant localRegistrant16 = this.mSmsOnSimRegistrant;
                                        AsyncResult localAsyncResult28 = new AsyncResult(null, arrayOfInt, null);
                                        localRegistrant16.notifyRegistrant(localAsyncResult28);
                                      }
                                    }
                                    else
                                    {
                                      riljLog(" NEW_SMS_ON_SIM ERROR with wrong length " + arrayOfInt.length);
                                      return;
                                      String[] arrayOfString1 = (String[])localObject2;
                                      if (arrayOfString1.length < 2)
                                      {
                                        arrayOfString1 = new String[2];
                                        arrayOfString1[0] = ((String[])(String[])localObject2)[0];
                                        arrayOfString1[1] = null;
                                      }
                                      unsljLogMore(i, arrayOfString1[0]);
                                      if (this.mUSSDRegistrant != null)
                                      {
                                        Registrant localRegistrant15 = this.mUSSDRegistrant;
                                        AsyncResult localAsyncResult27 = new AsyncResult(null, arrayOfString1, null);
                                        localRegistrant15.notifyRegistrant(localAsyncResult27);
                                        return;
                                        unsljLogRet(i, localObject2);
                                        long l = paramParcel.readLong();
                                        Object[] arrayOfObject = new Object[2];
                                        arrayOfObject[0] = localObject2;
                                        arrayOfObject[1] = Long.valueOf(l);
                                        if (SystemProperties.getBoolean("telephony.test.ignore.nitz", false))
                                        {
                                          riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
                                          return;
                                        }
                                        if (this.mNITZTimeRegistrant != null)
                                        {
                                          Registrant localRegistrant14 = this.mNITZTimeRegistrant;
                                          AsyncResult localAsyncResult26 = new AsyncResult(null, arrayOfObject, null);
                                          localRegistrant14.notifyRegistrant(localAsyncResult26);
                                          return;
                                        }
                                        this.mLastNITZTimeInfo = arrayOfObject;
                                        return;
                                        if (this.mSignalStrengthRegistrant != null)
                                        {
                                          Registrant localRegistrant13 = this.mSignalStrengthRegistrant;
                                          AsyncResult localAsyncResult25 = new AsyncResult(null, localObject2, null);
                                          localRegistrant13.notifyRegistrant(localAsyncResult25);
                                          return;
                                          unsljLogRet(i, localObject2);
                                          RegistrantList localRegistrantList12 = this.mDataCallListChangedRegistrants;
                                          AsyncResult localAsyncResult24 = new AsyncResult(null, localObject2, null);
                                          localRegistrantList12.notifyRegistrants(localAsyncResult24);
                                          return;
                                          unsljLogRet(i, localObject2);
                                          if (this.mSsnRegistrant != null)
                                          {
                                            Registrant localRegistrant12 = this.mSsnRegistrant;
                                            AsyncResult localAsyncResult23 = new AsyncResult(null, localObject2, null);
                                            localRegistrant12.notifyRegistrant(localAsyncResult23);
                                            return;
                                            unsljLog(i);
                                            if (this.mCatSessionEndRegistrant != null)
                                            {
                                              Registrant localRegistrant11 = this.mCatSessionEndRegistrant;
                                              AsyncResult localAsyncResult22 = new AsyncResult(null, localObject2, null);
                                              localRegistrant11.notifyRegistrant(localAsyncResult22);
                                              return;
                                              unsljLogRet(i, localObject2);
                                              if (this.mCatProCmdRegistrant != null)
                                              {
                                                Registrant localRegistrant10 = this.mCatProCmdRegistrant;
                                                AsyncResult localAsyncResult21 = new AsyncResult(null, localObject2, null);
                                                localRegistrant10.notifyRegistrant(localAsyncResult21);
                                                return;
                                                unsljLogRet(i, localObject2);
                                                if (this.mCatEventRegistrant != null)
                                                {
                                                  Registrant localRegistrant9 = this.mCatEventRegistrant;
                                                  AsyncResult localAsyncResult20 = new AsyncResult(null, localObject2, null);
                                                  localRegistrant9.notifyRegistrant(localAsyncResult20);
                                                  return;
                                                  unsljLogRet(i, localObject2);
                                                  if (this.mCatCallSetUpRegistrant != null)
                                                  {
                                                    Registrant localRegistrant8 = this.mCatCallSetUpRegistrant;
                                                    AsyncResult localAsyncResult19 = new AsyncResult(null, localObject2, null);
                                                    localRegistrant8.notifyRegistrant(localAsyncResult19);
                                                    return;
                                                    unsljLog(i);
                                                    if (this.mIccSmsFullRegistrant != null)
                                                    {
                                                      this.mIccSmsFullRegistrant.notifyRegistrant();
                                                      return;
                                                      unsljLogRet(i, localObject2);
                                                      if (this.mIccRefreshRegistrants != null)
                                                      {
                                                        RegistrantList localRegistrantList11 = this.mIccRefreshRegistrants;
                                                        AsyncResult localAsyncResult18 = new AsyncResult(null, localObject2, null);
                                                        localRegistrantList11.notifyRegistrants(localAsyncResult18);
                                                        return;
                                                        unsljLogRet(i, localObject2);
                                                        if (this.mRingRegistrant != null)
                                                        {
                                                          Registrant localRegistrant7 = this.mRingRegistrant;
                                                          AsyncResult localAsyncResult17 = new AsyncResult(null, localObject2, null);
                                                          localRegistrant7.notifyRegistrant(localAsyncResult17);
                                                          return;
                                                          unsljLogvRet(i, localObject2);
                                                          if (this.mRestrictedStateRegistrant != null)
                                                          {
                                                            Registrant localRegistrant6 = this.mRestrictedStateRegistrant;
                                                            AsyncResult localAsyncResult16 = new AsyncResult(null, localObject2, null);
                                                            localRegistrant6.notifyRegistrant(localAsyncResult16);
                                                            return;
                                                            unsljLog(i);
                                                            if (this.mIccStatusChangedRegistrants != null)
                                                            {
                                                              this.mIccStatusChangedRegistrants.notifyRegistrants();
                                                              return;
                                                              unsljLog(i);
                                                              SmsMessage localSmsMessage1 = (SmsMessage)localObject2;
                                                              if (this.mCdmaSmsRegistrant != null)
                                                              {
                                                                Registrant localRegistrant5 = this.mCdmaSmsRegistrant;
                                                                AsyncResult localAsyncResult15 = new AsyncResult(null, localSmsMessage1, null);
                                                                localRegistrant5.notifyRegistrant(localAsyncResult15);
                                                                return;
                                                                unsljLog(i);
                                                                if (this.mGsmBroadcastSmsRegistrant != null)
                                                                {
                                                                  Registrant localRegistrant4 = this.mGsmBroadcastSmsRegistrant;
                                                                  AsyncResult localAsyncResult14 = new AsyncResult(null, localObject2, null);
                                                                  localRegistrant4.notifyRegistrant(localAsyncResult14);
                                                                  return;
                                                                  unsljLog(i);
                                                                  if (this.mIccSmsFullRegistrant != null)
                                                                  {
                                                                    this.mIccSmsFullRegistrant.notifyRegistrant();
                                                                    return;
                                                                    unsljLog(i);
                                                                    if (this.mEmergencyCallbackModeRegistrant != null)
                                                                    {
                                                                      this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
                                                                      return;
                                                                      unsljLogRet(i, localObject2);
                                                                      if (this.mCallWaitingInfoRegistrants != null)
                                                                      {
                                                                        RegistrantList localRegistrantList10 = this.mCallWaitingInfoRegistrants;
                                                                        AsyncResult localAsyncResult13 = new AsyncResult(null, localObject2, null);
                                                                        localRegistrantList10.notifyRegistrants(localAsyncResult13);
                                                                        return;
                                                                        unsljLogRet(i, localObject2);
                                                                        if (this.mOtaProvisionRegistrants != null)
                                                                        {
                                                                          RegistrantList localRegistrantList9 = this.mOtaProvisionRegistrants;
                                                                          AsyncResult localAsyncResult12 = new AsyncResult(null, localObject2, null);
                                                                          localRegistrantList9.notifyRegistrants(localAsyncResult12);
                                                                          return;
                                                                          try
                                                                          {
                                                                            ArrayList localArrayList = (ArrayList)localObject2;
                                                                            Iterator localIterator = localArrayList.iterator();
                                                                            while (localIterator.hasNext())
                                                                            {
                                                                              CdmaInformationRecords localCdmaInformationRecords = (CdmaInformationRecords)localIterator.next();
                                                                              unsljLogRet(i, localCdmaInformationRecords);
                                                                              notifyRegistrantsCdmaInfoRec(localCdmaInformationRecords);
                                                                            }
                                                                          }
                                                                          catch (ClassCastException localClassCastException)
                                                                          {
                                                                            Log.e("RILJ", "Unexpected exception casting to listInfoRecs", localClassCastException);
                                                                            return;
                                                                          }
                                                                        }
                                                                      }
                                                                    }
                                                                  }
                                                                }
                                                              }
                                                            }
                                                          }
                                                        }
                                                      }
                                                    }
                                                  }
                                                }
                                              }
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                              unsljLogvRet(i, IccUtils.bytesToHexString((byte[])localObject2));
                            }
                            while (this.mUnsolOemHookRawRegistrant == null);
                            Registrant localRegistrant3 = this.mUnsolOemHookRawRegistrant;
                            AsyncResult localAsyncResult11 = new AsyncResult(null, localObject2, null);
                            localRegistrant3.notifyRegistrant(localAsyncResult11);
                            return;
                            unsljLogvRet(i, localObject2);
                          }
                          while (this.mRingbackToneRegistrants == null);
                          if (((int[])(int[])localObject2)[0] == 1);
                          for (boolean bool = true; ; bool = false)
                          {
                            this.mRingbackToneRegistrants.notifyRegistrants(new AsyncResult(null, Boolean.valueOf(bool), null));
                            return;
                          }
                          unsljLogRet(i, localObject2);
                        }
                        while (this.mResendIncallMuteRegistrants == null);
                        RegistrantList localRegistrantList8 = this.mResendIncallMuteRegistrants;
                        AsyncResult localAsyncResult10 = new AsyncResult(null, localObject2, null);
                        localRegistrantList8.notifyRegistrants(localAsyncResult10);
                        return;
                        unsljLogRet(i, localObject2);
                      }
                      while (this.mVoiceRadioTechChangedRegistrants == null);
                      RegistrantList localRegistrantList7 = this.mVoiceRadioTechChangedRegistrants;
                      AsyncResult localAsyncResult9 = new AsyncResult(null, localObject2, null);
                      localRegistrantList7.notifyRegistrants(localAsyncResult9);
                      return;
                      unsljLogRet(i, localObject2);
                    }
                    while (this.mCdmaSubscriptionChangedRegistrants == null);
                    RegistrantList localRegistrantList6 = this.mCdmaSubscriptionChangedRegistrants;
                    AsyncResult localAsyncResult8 = new AsyncResult(null, localObject2, null);
                    localRegistrantList6.notifyRegistrants(localAsyncResult8);
                    return;
                    unsljLogRet(i, localObject2);
                  }
                  while (this.mSSRegistrant == null);
                  Registrant localRegistrant2 = this.mSSRegistrant;
                  AsyncResult localAsyncResult7 = new AsyncResult(null, localObject2, null);
                  localRegistrant2.notifyRegistrant(localAsyncResult7);
                  return;
                  unsljLogRet(i, localObject2);
                }
                while (this.mCatCcAlphaRegistrant == null);
                Registrant localRegistrant1 = this.mCatCcAlphaRegistrant;
                AsyncResult localAsyncResult6 = new AsyncResult(null, localObject2, null);
                localRegistrant1.notifyRegistrant(localAsyncResult6);
                return;
                unsljLogRet(i, localObject2);
              }
              while (this.mCdmaPrlChangedRegistrants == null);
              RegistrantList localRegistrantList5 = this.mCdmaPrlChangedRegistrants;
              AsyncResult localAsyncResult5 = new AsyncResult(null, localObject2, null);
              localRegistrantList5.notifyRegistrants(localAsyncResult5);
              return;
              unsljLogRet(i, localObject2);
            }
            while (this.mExitEmergencyCallbackModeRegistrants == null);
            this.mExitEmergencyCallbackModeRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
            return;
            unsljLog(i);
            RegistrantList localRegistrantList4 = this.mModifyCallRegistrants;
            AsyncResult localAsyncResult4 = new AsyncResult(null, localObject2, null);
            localRegistrantList4.notifyRegistrants(localAsyncResult4);
            return;
            unsljLogRet(i, localObject2);
            notifyRegistrantsRilConnectionChanged(((int[])(int[])localObject2)[0]);
            return;
            unsljLogvRet(i, localObject2);
          }
          while (this.mTetheredModeStateRegistrants == null);
          if (localObject2 != null)
          {
            RegistrantList localRegistrantList3 = this.mTetheredModeStateRegistrants;
            AsyncResult localAsyncResult3 = new AsyncResult(null, localObject2, null);
            localRegistrantList3.notifyRegistrants(localAsyncResult3);
            return;
          }
          Log.e("RILJ", "null returned, expected non-null");
          return;
          unsljLogRet(i, localObject2);
        }
        while (this.mSubscriptionStatusRegistrants == null);
        RegistrantList localRegistrantList2 = this.mSubscriptionStatusRegistrants;
        AsyncResult localAsyncResult2 = new AsyncResult(null, localObject2, null);
        localRegistrantList2.notifyRegistrants(localAsyncResult2);
        return;
        unsljLogRet(i, localObject2);
      }
      while (this.mQosStateChangedIndRegistrants == null);
      RegistrantList localRegistrantList1 = this.mQosStateChangedIndRegistrants;
      AsyncResult localAsyncResult1 = new AsyncResult(null, localObject2, null);
      localRegistrantList1.notifyRegistrants(localAsyncResult1);
      return;
      unsljLogRet(i, localObject2);
    }
    while (this.mUnsolRplmnsStateRegistrant == null);
    this.mUnsolRplmnsStateRegistrant.notifyRegistrants(new AsyncResult(null, null, null));
    return;
    unsljLogRet(i, localObject2);
    IccCardApplicationStatus.setIccTypeHw(((int[])(int[])localObject2)[0]);
  }

  private static int readRilMessage(InputStream paramInputStream, byte[] paramArrayOfByte)
    throws IOException
  {
    int i = 0;
    int j = 4;
    do
    {
      int k = paramInputStream.read(paramArrayOfByte, i, j);
      if (k < 0)
      {
        Log.e("RILJ", "Hit EOS reading message length");
        return -1;
      }
      i += k;
      j -= k;
    }
    while (j > 0);
    int m = (0xFF & paramArrayOfByte[0]) << 24 | (0xFF & paramArrayOfByte[1]) << 16 | (0xFF & paramArrayOfByte[2]) << 8 | 0xFF & paramArrayOfByte[3];
    int n = 0;
    int i1 = m;
    do
    {
      int i2 = paramInputStream.read(paramArrayOfByte, n, i1);
      if (i2 < 0)
      {
        Log.e("RILJ", "Hit EOS reading message.  messageLength=" + m + " remaining=" + i1);
        return -1;
      }
      n += i2;
      i1 -= i2;
    }
    while (i1 > 0);
    return m;
  }

  private void releaseWakeLockIfDone()
  {
    synchronized (this.mWakeLock)
    {
      if ((this.mWakeLock.isHeld()) && (this.mRequestMessagesPending == 0) && (this.mRequestMessagesWaiting == 0))
      {
        this.mSender.removeMessages(2);
        this.mWakeLock.release();
      }
      return;
    }
  }

  static String requestToString(int paramInt)
  {
    switch (paramInt)
    {
    default:
      return "<unknown request>";
    case 1:
      return "GET_SIM_STATUS";
    case 2:
      return "ENTER_SIM_PIN";
    case 3:
      return "ENTER_SIM_PUK";
    case 4:
      return "ENTER_SIM_PIN2";
    case 5:
      return "ENTER_SIM_PUK2";
    case 6:
      return "CHANGE_SIM_PIN";
    case 7:
      return "CHANGE_SIM_PIN2";
    case 8:
      return "ENTER_NETWORK_DEPERSONALIZATION";
    case 9:
      return "GET_CURRENT_CALLS";
    case 10:
      return "DIAL";
    case 11:
      return "GET_IMSI";
    case 12:
      return "HANGUP";
    case 13:
      return "HANGUP_WAITING_OR_BACKGROUND";
    case 14:
      return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
    case 15:
      return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
    case 16:
      return "CONFERENCE";
    case 17:
      return "UDUB";
    case 18:
      return "LAST_CALL_FAIL_CAUSE";
    case 19:
      return "SIGNAL_STRENGTH";
    case 20:
      return "VOICE_REGISTRATION_STATE";
    case 21:
      return "DATA_REGISTRATION_STATE";
    case 22:
      return "OPERATOR";
    case 23:
      return "RADIO_POWER";
    case 24:
      return "DTMF";
    case 25:
      return "SEND_SMS";
    case 513:
      return "SETUP_QOS";
    case 515:
      return "GET_QOS_STATUS";
    case 514:
      return "RELEASE_QOS";
    case 516:
      return "MODIFY_QOS";
    case 517:
      return "SUSPEND_QOS";
    case 518:
      return "RESUME_QOS";
    case 26:
      return "SEND_SMS_EXPECT_MORE";
    case 27:
      return "SETUP_DATA_CALL";
    case 28:
      return "SIM_IO";
    case 523:
      return "SIM_TRANSMIT_BASIC";
    case 524:
      return "SIM_OPEN_CHANNEL";
    case 525:
      return "SIM_CLOSE_CHANNEL";
    case 526:
      return "SIM_TRANSMIT_CHANNEL";
    case 29:
      return "SEND_USSD";
    case 30:
      return "CANCEL_USSD";
    case 31:
      return "GET_CLIR";
    case 32:
      return "SET_CLIR";
    case 33:
      return "QUERY_CALL_FORWARD_STATUS";
    case 34:
      return "SET_CALL_FORWARD";
    case 35:
      return "QUERY_CALL_WAITING";
    case 36:
      return "SET_CALL_WAITING";
    case 37:
      return "SMS_ACKNOWLEDGE";
    case 38:
      return "GET_IMEI";
    case 39:
      return "GET_IMEISV";
    case 40:
      return "ANSWER";
    case 41:
      return "DEACTIVATE_DATA_CALL";
    case 42:
      return "QUERY_FACILITY_LOCK";
    case 43:
      return "SET_FACILITY_LOCK";
    case 44:
      return "CHANGE_BARRING_PASSWORD";
    case 45:
      return "QUERY_NETWORK_SELECTION_MODE";
    case 46:
      return "SET_NETWORK_SELECTION_AUTOMATIC";
    case 47:
      return "SET_NETWORK_SELECTION_MANUAL";
    case 48:
      return "QUERY_AVAILABLE_NETWORKS ";
    case 49:
      return "DTMF_START";
    case 50:
      return "DTMF_STOP";
    case 51:
      return "BASEBAND_VERSION";
    case 52:
      return "SEPARATE_CONNECTION";
    case 53:
      return "SET_MUTE";
    case 54:
      return "GET_MUTE";
    case 55:
      return "QUERY_CLIP";
    case 56:
      return "LAST_DATA_CALL_FAIL_CAUSE";
    case 57:
      return "DATA_CALL_LIST";
    case 58:
      return "RESET_RADIO";
    case 59:
      return "OEM_HOOK_RAW";
    case 60:
      return "OEM_HOOK_STRINGS";
    case 61:
      return "SCREEN_STATE";
    case 62:
      return "SET_SUPP_SVC_NOTIFICATION";
    case 63:
      return "WRITE_SMS_TO_SIM";
    case 64:
      return "DELETE_SMS_ON_SIM";
    case 65:
      return "SET_BAND_MODE";
    case 66:
      return "QUERY_AVAILABLE_BAND_MODE";
    case 67:
      return "REQUEST_STK_GET_PROFILE";
    case 68:
      return "REQUEST_STK_SET_PROFILE";
    case 69:
      return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
    case 70:
      return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
    case 71:
      return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
    case 72:
      return "REQUEST_EXPLICIT_CALL_TRANSFER";
    case 73:
      return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
    case 74:
      return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
    case 75:
      return "REQUEST_GET_NEIGHBORING_CELL_IDS";
    case 76:
      return "REQUEST_SET_LOCATION_UPDATES";
    case 77:
      return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
    case 78:
      return "RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE";
    case 79:
      return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
    case 80:
      return "RIL_REQUEST_SET_TTY_MODE";
    case 81:
      return "RIL_REQUEST_QUERY_TTY_MODE";
    case 82:
      return "RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
    case 83:
      return "RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
    case 84:
      return "RIL_REQUEST_CDMA_FLASH";
    case 85:
      return "RIL_REQUEST_CDMA_BURST_DTMF";
    case 87:
      return "RIL_REQUEST_CDMA_SEND_SMS";
    case 88:
      return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
    case 89:
      return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
    case 90:
      return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
    case 92:
      return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
    case 93:
      return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
    case 91:
      return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
    case 86:
      return "RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY";
    case 94:
      return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
    case 95:
      return "RIL_REQUEST_CDMA_SUBSCRIPTION";
    case 96:
      return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
    case 97:
      return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
    case 98:
      return "RIL_REQUEST_DEVICE_IDENTITY";
    case 100:
      return "RIL_REQUEST_GET_SMSC_ADDRESS";
    case 101:
      return "RIL_REQUEST_SET_SMSC_ADDRESS";
    case 99:
      return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
    case 102:
      return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
    case 103:
      return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
    case 104:
      return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
    case 506:
      return "RIL_REQUEST_GET_DATA_CALL_PROFILE";
    case 105:
      return "RIL_REQUEST_ISIM_AUTHENTICATION";
    case 108:
      return "RIL_REQUEST_VOICE_RADIO_TECH";
    case 504:
      return "RIL_REQUEST_IMS_REGISTRATION_STATE";
    case 505:
      return "RIL_REQUEST_IMS_SEND_SMS";
    case 512:
      return "RIL_REQUEST_SET_TRANSMIT_POWER";
    case 507:
      return "RIL_REQUEST_SET_UICC_SUBSCRIPTION";
    case 508:
      return "RIL_REQUEST_SET_DATA_SUBSCRIPTION";
    case 509:
      return "RIL_REQUEST_GET_UICC_SUBSCRIPTION";
    case 510:
      return "RIL_REQUEST_GET_DATA_SUBSCRIPTION";
    case 511:
      return "RIL_REQUEST_SET_SUBSCRIPTION_MODE";
    case 106:
      return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
    case 107:
      return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
    case 503:
      return "RIL_REQUEST_SET_SIM_LESS";
    case 501:
      return "RIL_REQUEST_SET_EMERGENCY_NUMBERS";
    case 502:
      return "MODEM_POWER";
    case 519:
      return "RIL_REQUEST_MODIFY_CALL_INITIATE";
    case 520:
      return "RIL_REQUEST_MODIFY_CALL_CONFIRM";
    case 521:
      return "RIL_REQUEST_RESTRAT_RILD";
    case 522:
      return "RIL_REQUEST_SWITCH_MTKSIM";
    case 536:
    }
    return "RIL_REQUEST_RESET_ALL_CONNECTIONS";
  }

  private Object responseCallForward(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    CallForwardInfo[] arrayOfCallForwardInfo = new CallForwardInfo[i];
    for (int j = 0; j < i; j++)
    {
      arrayOfCallForwardInfo[j] = new CallForwardInfo();
      arrayOfCallForwardInfo[j].status = paramParcel.readInt();
      arrayOfCallForwardInfo[j].reason = paramParcel.readInt();
      arrayOfCallForwardInfo[j].serviceClass = paramParcel.readInt();
      arrayOfCallForwardInfo[j].toa = paramParcel.readInt();
      arrayOfCallForwardInfo[j].number = paramParcel.readString();
      arrayOfCallForwardInfo[j].timeSeconds = paramParcel.readInt();
    }
    return arrayOfCallForwardInfo;
  }

  private Object responseCallList(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    ArrayList localArrayList = new ArrayList(i);
    int j = 0;
    if (j < i)
    {
      DriverCall localDriverCall = new DriverCall();
      localDriverCall.state = DriverCall.stateFromCLCC(paramParcel.readInt());
      localDriverCall.index = paramParcel.readInt();
      localDriverCall.TOA = paramParcel.readInt();
      boolean bool1;
      label72: boolean bool2;
      label89: boolean bool3;
      label115: boolean bool4;
      if (paramParcel.readInt() != 0)
      {
        bool1 = true;
        localDriverCall.isMpty = bool1;
        if (paramParcel.readInt() == 0)
          break label433;
        bool2 = true;
        localDriverCall.isMT = bool2;
        localDriverCall.als = paramParcel.readInt();
        if (paramParcel.readInt() != 0)
          break label439;
        bool3 = false;
        localDriverCall.isVoice = bool3;
        if (paramParcel.readInt() == 0)
          break label445;
        bool4 = true;
        label132: localDriverCall.isVoicePrivacy = bool4;
        localDriverCall.number = paramParcel.readString();
        localDriverCall.numberPresentation = DriverCall.presentationFromCLIP(paramParcel.readInt());
        localDriverCall.name = paramParcel.readString();
        localDriverCall.namePresentation = paramParcel.readInt();
        if (paramParcel.readInt() != 1)
          break label451;
        localDriverCall.uusInfo = new UUSInfo();
        localDriverCall.uusInfo.setType(paramParcel.readInt());
        localDriverCall.uusInfo.setDcs(paramParcel.readInt());
        byte[] arrayOfByte = paramParcel.createByteArray();
        localDriverCall.uusInfo.setUserData(arrayOfByte);
        Object[] arrayOfObject = new Object[3];
        arrayOfObject[0] = Integer.valueOf(localDriverCall.uusInfo.getType());
        arrayOfObject[1] = Integer.valueOf(localDriverCall.uusInfo.getDcs());
        arrayOfObject[2] = Integer.valueOf(localDriverCall.uusInfo.getUserData().length);
        riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", arrayOfObject));
        riljLogv("Incoming UUS : data (string)=" + new String(localDriverCall.uusInfo.getUserData()));
        riljLogv("Incoming UUS : data (hex): " + IccUtils.bytesToHexString(localDriverCall.uusInfo.getUserData()));
        label374: localDriverCall.number = PhoneNumberUtils.stringFromStringAndTOA(localDriverCall.number, localDriverCall.TOA);
        localArrayList.add(localDriverCall);
        if (!localDriverCall.isVoicePrivacy)
          break label461;
        this.mVoicePrivacyOnRegistrants.notifyRegistrants();
        riljLog("InCall VoicePrivacy is enabled");
      }
      while (true)
      {
        j++;
        break;
        bool1 = false;
        break label72;
        label433: bool2 = false;
        break label89;
        label439: bool3 = true;
        break label115;
        label445: bool4 = false;
        break label132;
        label451: riljLogv("Incoming UUS : NOT present!");
        break label374;
        label461: this.mVoicePrivacyOffRegistrants.notifyRegistrants();
        riljLog("InCall VoicePrivacy is disabled");
      }
    }
    Collections.sort(localArrayList);
    if ((i == 0) && (this.mTestingEmergencyCall.getAndSet(false)) && (this.mEmergencyCallbackModeRegistrant != null))
    {
      riljLog("responseCallList: call ended, testing emergency call, notify ECM Registrants");
      this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
    }
    return localArrayList;
  }

  private Object responseCallRing(Parcel paramParcel)
  {
    char[] arrayOfChar = new char[4];
    arrayOfChar[0] = ((char)paramParcel.readInt());
    arrayOfChar[1] = ((char)paramParcel.readInt());
    arrayOfChar[2] = ((char)paramParcel.readInt());
    arrayOfChar[3] = ((char)paramParcel.readInt());
    return arrayOfChar;
  }

  private Object responseCdmaBroadcastConfig(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    if (i == 0)
    {
      arrayOfInt = new int[94];
      arrayOfInt[0] = 31;
      for (int m = 1; m < 94; m += 3)
      {
        arrayOfInt[(m + 0)] = (m / 3);
        arrayOfInt[(m + 1)] = 1;
        arrayOfInt[(m + 2)] = 0;
      }
    }
    int j = 1 + i * 3;
    int[] arrayOfInt = new int[j];
    arrayOfInt[0] = i;
    for (int k = 1; k < j; k++)
      arrayOfInt[k] = paramParcel.readInt();
    return arrayOfInt;
  }

  private Object responseCdmaCallWaiting(Parcel paramParcel)
  {
    CdmaCallWaitingNotification localCdmaCallWaitingNotification = new CdmaCallWaitingNotification();
    localCdmaCallWaitingNotification.number = paramParcel.readString();
    localCdmaCallWaitingNotification.numberPresentation = CdmaCallWaitingNotification.presentationFromCLIP(paramParcel.readInt());
    localCdmaCallWaitingNotification.name = paramParcel.readString();
    localCdmaCallWaitingNotification.namePresentation = localCdmaCallWaitingNotification.numberPresentation;
    localCdmaCallWaitingNotification.isPresent = paramParcel.readInt();
    localCdmaCallWaitingNotification.signalType = paramParcel.readInt();
    localCdmaCallWaitingNotification.alertPitch = paramParcel.readInt();
    localCdmaCallWaitingNotification.signal = paramParcel.readInt();
    localCdmaCallWaitingNotification.numberType = paramParcel.readInt();
    localCdmaCallWaitingNotification.numberPlan = paramParcel.readInt();
    return localCdmaCallWaitingNotification;
  }

  private ArrayList<CdmaInformationRecords> responseCdmaInformationRecord(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    ArrayList localArrayList = new ArrayList(i);
    for (int j = 0; j < i; j++)
      localArrayList.add(new CdmaInformationRecords(paramParcel));
    return localArrayList;
  }

  private Object responseCdmaSms(Parcel paramParcel)
  {
    return SmsMessage.newFromParcel(paramParcel);
  }

  private Object responseCellList(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    ArrayList localArrayList = new ArrayList();
    String str = SystemProperties.get("gsm.network.type", "unknown");
    int j;
    if (str.equals("GPRS"))
      j = 1;
    while (j != 0)
    {
      for (int k = 0; k < i; k++)
        localArrayList.add(new NeighboringCellInfo(paramParcel.readInt(), paramParcel.readString(), j));
      if (str.equals("EDGE"))
        j = 2;
      else if (str.equals("UMTS"))
        j = 3;
      else if (str.equals("HSDPA"))
        j = 8;
      else if (str.equals("HSUPA"))
        j = 9;
      else if (str.equals("HSPA"))
        j = 10;
      else
        j = 0;
    }
    return localArrayList;
  }

  private Object responseDataCallList(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    int j = paramParcel.readInt();
    riljLog("responseDataCallList ver=" + i + " num=" + j);
    ArrayList localArrayList = new ArrayList(j);
    for (int k = 0; k < j; k++)
      localArrayList.add(getDataCallState(paramParcel, i));
    return localArrayList;
  }

  private ArrayList<DataProfile> responseGetDataCallProfile(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    riljLog("# data call profiles:" + i);
    ArrayList localArrayList = new ArrayList(i);
    for (int j = 0; j < i; j++)
    {
      DataProfileOmh localDataProfileOmh = new DataProfileOmh(paramParcel.readInt(), paramParcel.readInt());
      riljLog("responseGetDataCallProfile()" + localDataProfileOmh.getProfileId() + ":" + localDataProfileOmh.getPriority());
      localArrayList.add(localDataProfileOmh);
    }
    return localArrayList;
  }

  private Object responseGetPreferredNetworkType(Parcel paramParcel)
  {
    int[] arrayOfInt = (int[])responseInts(paramParcel);
    if (arrayOfInt.length >= 1)
      this.mPreferredNetworkType = arrayOfInt[0];
    return arrayOfInt;
  }

  private Object responseGmsBroadcastConfig(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    ArrayList localArrayList = new ArrayList(i);
    int j = 0;
    if (j < i)
    {
      int k = paramParcel.readInt();
      int m = paramParcel.readInt();
      int n = paramParcel.readInt();
      int i1 = paramParcel.readInt();
      if (paramParcel.readInt() == 1);
      for (boolean bool = true; ; bool = false)
      {
        localArrayList.add(new SmsBroadcastConfigInfo(k, m, n, i1, bool));
        j++;
        break;
      }
    }
    return localArrayList;
  }

  private Object responseICC_IO(Parcel paramParcel)
  {
    return new IccIoResult(paramParcel.readInt(), paramParcel.readInt(), paramParcel.readString());
  }

  private Object responseIccCardStatus(Parcel paramParcel)
  {
    IccCardStatus localIccCardStatus = new IccCardStatus();
    localIccCardStatus.setCardState(paramParcel.readInt());
    localIccCardStatus.setUniversalPinState(paramParcel.readInt());
    localIccCardStatus.mGsmUmtsSubscriptionAppIndex = paramParcel.readInt();
    localIccCardStatus.mCdmaSubscriptionAppIndex = paramParcel.readInt();
    localIccCardStatus.mImsSubscriptionAppIndex = paramParcel.readInt();
    int i = paramParcel.readInt();
    if (i > 8)
      i = 8;
    localIccCardStatus.mApplications = new IccCardApplicationStatus[i];
    for (int j = 0; j < i; j++)
    {
      IccCardApplicationStatus localIccCardApplicationStatus = new IccCardApplicationStatus();
      localIccCardApplicationStatus.app_type = localIccCardApplicationStatus.AppTypeFromRILInt(paramParcel.readInt());
      localIccCardApplicationStatus.app_state = localIccCardApplicationStatus.AppStateFromRILInt(paramParcel.readInt());
      localIccCardApplicationStatus.perso_substate = localIccCardApplicationStatus.PersoSubstateFromRILInt(paramParcel.readInt());
      localIccCardApplicationStatus.aid = paramParcel.readString();
      localIccCardApplicationStatus.app_label = paramParcel.readString();
      localIccCardApplicationStatus.pin1_replaced = paramParcel.readInt();
      localIccCardApplicationStatus.pin1 = localIccCardApplicationStatus.PinStateFromRILInt(paramParcel.readInt());
      localIccCardApplicationStatus.pin2 = localIccCardApplicationStatus.PinStateFromRILInt(paramParcel.readInt());
      localIccCardStatus.mApplications[j] = localIccCardApplicationStatus;
    }
    return localIccCardStatus;
  }

  private Object responseInts(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    int[] arrayOfInt = new int[i];
    for (int j = 0; j < i; j++)
      arrayOfInt[j] = paramParcel.readInt();
    return arrayOfInt;
  }

  private Object responseModifyCall(Parcel paramParcel)
  {
    CallModify localCallModify = new CallModify();
    localCallModify.call_index = paramParcel.readInt();
    localCallModify.call_details.call_type = paramParcel.readInt();
    localCallModify.call_details.call_domain = paramParcel.readInt();
    localCallModify.call_details.extras = paramParcel.createStringArray();
    return localCallModify;
  }

  private Object responseOperatorInfos(Parcel paramParcel)
  {
    String[] arrayOfString = (String[])responseStrings(paramParcel);
    if (arrayOfString.length % 4 != 0)
      throw new RuntimeException("RIL_REQUEST_QUERY_AVAILABLE_NETWORKS: invalid response. Got " + arrayOfString.length + " strings, expected multible of 4");
    ArrayList localArrayList = new ArrayList(arrayOfString.length / 4);
    for (int i = 0; i < arrayOfString.length; i += 4)
      localArrayList.add(new OperatorInfo(arrayOfString[(i + 0)], arrayOfString[(i + 1)], arrayOfString[(i + 2)], arrayOfString[(i + 3)]));
    return localArrayList;
  }

  private Object responseRaw(Parcel paramParcel)
  {
    return paramParcel.createByteArray();
  }

  private Object responseSMS(Parcel paramParcel)
  {
    return new SmsResponse(paramParcel.readInt(), paramParcel.readString(), paramParcel.readInt());
  }

  private Object responseSSData(Parcel paramParcel)
  {
    SsData localSsData = new SsData();
    localSsData.serviceType = localSsData.ServiceTypeFromRILInt(paramParcel.readInt());
    localSsData.requestType = localSsData.RequestTypeFromRILInt(paramParcel.readInt());
    localSsData.teleserviceType = localSsData.TeleserviceTypeFromRILInt(paramParcel.readInt());
    localSsData.serviceClass = paramParcel.readInt();
    localSsData.result = paramParcel.readInt();
    int i = paramParcel.readInt();
    int k;
    if ((localSsData.serviceType.isTypeCF()) && (localSsData.requestType.isTypeInterrogation()))
    {
      localSsData.cfInfo = new CallForwardInfo[i];
      k = 0;
    }
    while (k < i)
    {
      localSsData.cfInfo[k] = new CallForwardInfo();
      localSsData.cfInfo[k].status = paramParcel.readInt();
      localSsData.cfInfo[k].reason = paramParcel.readInt();
      localSsData.cfInfo[k].serviceClass = paramParcel.readInt();
      localSsData.cfInfo[k].toa = paramParcel.readInt();
      localSsData.cfInfo[k].number = paramParcel.readString();
      localSsData.cfInfo[k].timeSeconds = paramParcel.readInt();
      riljLog("[SS Data] CF Info " + k + " : " + localSsData.cfInfo[k]);
      k++;
      continue;
      localSsData.ssInfo = new int[i];
      for (int j = 0; j < i; j++)
      {
        localSsData.ssInfo[j] = paramParcel.readInt();
        riljLog("[SS Data] SS Info " + j + " : " + localSsData.ssInfo[j]);
      }
    }
    return localSsData;
  }

  private Object responseSetupDataCall(Parcel paramParcel)
  {
    int i = paramParcel.readInt();
    int j = paramParcel.readInt();
    if (i < 5)
    {
      DataCallState localDataCallState = new DataCallState();
      localDataCallState.version = i;
      localDataCallState.cid = Integer.parseInt(paramParcel.readString());
      localDataCallState.ifname = paramParcel.readString();
      if (TextUtils.isEmpty(localDataCallState.ifname))
        throw new RuntimeException("RIL_REQUEST_SETUP_DATA_CALL response, no ifname");
      String str1 = paramParcel.readString();
      if (!TextUtils.isEmpty(str1))
        localDataCallState.addresses = str1.split(" ");
      if (j >= 4)
      {
        String str3 = paramParcel.readString();
        riljLog("responseSetupDataCall got dnses=" + str3);
        if (!TextUtils.isEmpty(str3))
          localDataCallState.dnses = str3.split(" ");
      }
      if (j >= 5)
      {
        String str2 = paramParcel.readString();
        riljLog("responseSetupDataCall got gateways=" + str2);
        if (!TextUtils.isEmpty(str2))
          localDataCallState.gateways = str2.split(" ");
      }
      return localDataCallState;
    }
    if (j != 1)
      throw new RuntimeException("RIL_REQUEST_SETUP_DATA_CALL response expecting 1 RIL_Data_Call_response_v5 got " + j);
    return getDataCallState(paramParcel, i);
  }

  private Object responseSignalStrength(Parcel paramParcel)
  {
    return new SignalStrength(paramParcel);
  }

  private Object responseSimRefresh(Parcel paramParcel)
  {
    IccRefreshResponse localIccRefreshResponse = new IccRefreshResponse();
    localIccRefreshResponse.refreshResult = paramParcel.readInt();
    localIccRefreshResponse.efId = paramParcel.readInt();
    localIccRefreshResponse.aid = paramParcel.readString();
    return localIccRefreshResponse;
  }

  private Object responseString(Parcel paramParcel)
  {
    return paramParcel.readString();
  }

  private Object responseStrings(Parcel paramParcel)
  {
    return paramParcel.readStringArray();
  }

  private Object responseSuppServiceNotification(Parcel paramParcel)
  {
    SuppServiceNotification localSuppServiceNotification = new SuppServiceNotification();
    localSuppServiceNotification.notificationType = paramParcel.readInt();
    localSuppServiceNotification.code = paramParcel.readInt();
    localSuppServiceNotification.index = paramParcel.readInt();
    localSuppServiceNotification.type = paramParcel.readInt();
    localSuppServiceNotification.number = paramParcel.readString();
    return localSuppServiceNotification;
  }

  static String responseToString(int paramInt)
  {
    switch (paramInt)
    {
    default:
      return "<unknown response>";
    case 1000:
      return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
    case 1001:
      return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
    case 1002:
      return "UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED";
    case 1003:
      return "UNSOL_RESPONSE_NEW_SMS";
    case 1004:
      return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
    case 1005:
      return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
    case 1006:
      return "UNSOL_ON_USSD";
    case 1007:
      return "UNSOL_ON_USSD_REQUEST";
    case 1008:
      return "UNSOL_NITZ_TIME_RECEIVED";
    case 1009:
      return "UNSOL_SIGNAL_STRENGTH";
    case 1010:
      return "UNSOL_DATA_CALL_LIST_CHANGED";
    case 1011:
      return "UNSOL_SUPP_SVC_NOTIFICATION";
    case 1012:
      return "UNSOL_STK_SESSION_END";
    case 1013:
      return "UNSOL_STK_PROACTIVE_COMMAND";
    case 1014:
      return "UNSOL_STK_EVENT_NOTIFY";
    case 1015:
      return "UNSOL_STK_CALL_SETUP";
    case 1016:
      return "UNSOL_SIM_SMS_STORAGE_FULL";
    case 1017:
      return "UNSOL_SIM_REFRESH";
    case 1018:
      return "UNSOL_CALL_RING";
    case 1019:
      return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
    case 1020:
      return "UNSOL_RESPONSE_CDMA_NEW_SMS";
    case 1021:
      return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
    case 1022:
      return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
    case 1023:
      return "UNSOL_RESTRICTED_STATE_CHANGED";
    case 1024:
      return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
    case 1025:
      return "UNSOL_CDMA_CALL_WAITING";
    case 1026:
      return "UNSOL_CDMA_OTA_PROVISION_STATUS";
    case 1027:
      return "UNSOL_CDMA_INFO_REC";
    case 1028:
      return "UNSOL_OEM_HOOK_RAW";
    case 1029:
      return "UNSOL_RINGBACK_TONE";
    case 1030:
      return "UNSOL_RESEND_INCALL_MUTE";
    case 1031:
      return "CDMA_SUBSCRIPTION_SOURCE_CHANGED";
    case 1032:
      return "UNSOL_CDMA_PRL_CHANGED";
    case 1033:
      return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
    case 1510:
      return "UNSOL_MODIFY_CALL";
    case 1034:
      return "UNSOL_RIL_CONNECTED";
    case 1501:
      return "UNSOL_RESIDENT_NETWORK_CHANGED";
    case 1035:
      return "UNSOL_VOICE_RADIO_TECH_CHANGED";
    case 1505:
      return "RIL_UNSOL_DATA_NETWORK_STATE_CHANGED";
    case 1503:
      return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
    case 1504:
      return "RIL_UNSOL_TETHERED_MODE_STATE_CHANGED";
    case 1506:
      return "UNSOL_ON_SS";
    case 1507:
      return "UNSOL_STK_CC_ALPHA_NOTIFY";
    case 1509:
      return "RIL_UNSOL_QOS_STATE_CHANGED";
    case 1508:
      return "RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED";
    case 1502:
    }
    return "UNSOL_RESPONSE_SIM_TYPE";
  }

  private Object responseUiccSubscription(Parcel paramParcel)
  {
    return null;
  }

  private Object responseVoid(Parcel paramParcel)
  {
    return null;
  }

  private String retToString(int paramInt, Object paramObject)
  {
    if (paramObject == null)
      return "";
    int[] arrayOfInt;
    int m;
    StringBuilder localStringBuilder5;
    int n;
    switch (paramInt)
    {
    default:
      if (!(paramObject instanceof int[]))
        break label157;
      arrayOfInt = (int[])paramObject;
      m = arrayOfInt.length;
      localStringBuilder5 = new StringBuilder("{");
      if (m > 0)
      {
        n = 0 + 1;
        localStringBuilder5.append(arrayOfInt[0]);
      }
      break;
    case 11:
    case 38:
    case 39:
      while (n < m)
      {
        StringBuilder localStringBuilder6 = localStringBuilder5.append(", ");
        int i1 = n + 1;
        localStringBuilder6.append(arrayOfInt[n]);
        n = i1;
        continue;
        return "";
      }
    }
    localStringBuilder5.append("}");
    return localStringBuilder5.toString();
    label157: if ((paramObject instanceof String[]))
    {
      String[] arrayOfString = (String[])paramObject;
      int i = arrayOfString.length;
      StringBuilder localStringBuilder3 = new StringBuilder("{");
      if (i > 0)
      {
        int j = 0 + 1;
        localStringBuilder3.append(arrayOfString[0]);
        while (j < i)
        {
          StringBuilder localStringBuilder4 = localStringBuilder3.append(", ");
          int k = j + 1;
          localStringBuilder4.append(arrayOfString[j]);
          j = k;
        }
      }
      localStringBuilder3.append("}");
      return localStringBuilder3.toString();
    }
    if (paramInt == 9)
    {
      ArrayList localArrayList2 = (ArrayList)paramObject;
      StringBuilder localStringBuilder2 = new StringBuilder(" ");
      Iterator localIterator2 = localArrayList2.iterator();
      while (localIterator2.hasNext())
      {
        DriverCall localDriverCall = (DriverCall)localIterator2.next();
        localStringBuilder2.append("[").append(localDriverCall).append("] ");
      }
      return localStringBuilder2.toString();
    }
    if (paramInt == 75)
    {
      ArrayList localArrayList1 = (ArrayList)paramObject;
      StringBuilder localStringBuilder1 = new StringBuilder(" ");
      Iterator localIterator1 = localArrayList1.iterator();
      while (localIterator1.hasNext())
        localStringBuilder1.append((NeighboringCellInfo)localIterator1.next()).append(" ");
      return localStringBuilder1.toString();
    }
    return paramObject.toString();
  }

  private void riljLog(String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder().append(paramString);
    if (this.mInstanceId != null);
    for (String str = " [SUB" + this.mInstanceId + "]"; ; str = "")
    {
      Log.d("RILJ", str);
      return;
    }
  }

  private void riljLogv(String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder().append(paramString);
    if (this.mInstanceId != null);
    for (String str = " [SUB" + this.mInstanceId + "]"; ; str = "")
    {
      Log.v("RILJ", str);
      return;
    }
  }

  private void send(RILRequest paramRILRequest)
  {
    if (this.mSocket == null)
    {
      paramRILRequest.onError(1, null);
      paramRILRequest.release();
      return;
    }
    Message localMessage = this.mSender.obtainMessage(1, paramRILRequest);
    acquireWakeLock();
    localMessage.sendToTarget();
  }

  private void sendScreenState(boolean paramBoolean)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(61, null);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    while (true)
    {
      localParcel.writeInt(i);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + ": " + paramBoolean);
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  private void setCallModify(RILRequest paramRILRequest, CallModify paramCallModify)
  {
    paramRILRequest.mp.writeInt(paramCallModify.call_index);
    paramRILRequest.mp.writeInt(paramCallModify.call_details.call_type);
    paramRILRequest.mp.writeInt(paramCallModify.call_details.call_domain);
    paramRILRequest.mp.writeStringArray(paramCallModify.call_details.extras);
  }

  private void switchToRadioState(CommandsInterface.RadioState paramRadioState)
  {
    setRadioState(paramRadioState);
  }

  private int translateStatus(int paramInt)
  {
    switch (paramInt & 0x7)
    {
    case 1:
    case 2:
    case 4:
    case 6:
    default:
      return 1;
    case 3:
      return 0;
    case 5:
      return 3;
    case 7:
    }
    return 2;
  }

  private void unsljLog(int paramInt)
  {
    riljLog("[UNSL]< " + responseToString(paramInt));
  }

  private void unsljLogMore(int paramInt, String paramString)
  {
    riljLog("[UNSL]< " + responseToString(paramInt) + " " + paramString);
  }

  private void unsljLogRet(int paramInt, Object paramObject)
  {
    riljLog("[UNSL]< " + responseToString(paramInt) + " " + retToString(paramInt, paramObject));
  }

  private void unsljLogvRet(int paramInt, Object paramObject)
  {
    riljLogv("[UNSL]< " + responseToString(paramInt) + " " + retToString(paramInt, paramObject));
  }

  public void acceptCall(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(40, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void acknowledgeIncomingGsmSmsWithPdu(boolean paramBoolean, String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(106, paramMessage);
    localRILRequest.mp.writeInt(2);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    for (String str = "1"; ; str = "0")
    {
      localParcel.writeString(str);
      localRILRequest.mp.writeString(paramString);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + ' ' + paramBoolean + " [" + paramString + ']');
      send(localRILRequest);
      return;
    }
  }

  public void acknowledgeLastIncomingCdmaSms(boolean paramBoolean, int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(88, paramMessage);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    for (int i = 0; ; i = 1)
    {
      localParcel.writeInt(i);
      localRILRequest.mp.writeInt(paramInt);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramBoolean + " " + paramInt);
      send(localRILRequest);
      return;
    }
  }

  public void acknowledgeLastIncomingGsmSms(boolean paramBoolean, int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(37, paramMessage);
    localRILRequest.mp.writeInt(2);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    for (int i = 1; ; i = 0)
    {
      localParcel.writeInt(i);
      localRILRequest.mp.writeInt(paramInt);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramBoolean + " " + paramInt);
      send(localRILRequest);
      return;
    }
  }

  public void cancelPendingUssd(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(30, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void changeBarringPassword(String paramString1, String paramString2, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(44, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(3);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    send(localRILRequest);
  }

  public void changeIccPin(String paramString1, String paramString2, Message paramMessage)
  {
    changeIccPinForApp(paramString1, paramString2, null, paramMessage);
  }

  public void changeIccPin2(String paramString1, String paramString2, Message paramMessage)
  {
    changeIccPin2ForApp(paramString1, paramString2, null, paramMessage);
  }

  public void changeIccPin2ForApp(String paramString1, String paramString2, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(7, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(3);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    send(localRILRequest);
  }

  public void changeIccPinForApp(String paramString1, String paramString2, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(6, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(3);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    send(localRILRequest);
  }

  public void conference(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(16, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void deactivateDataCall(int paramInt1, int paramInt2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(41, paramMessage);
    localRILRequest.mp.writeInt(2);
    localRILRequest.mp.writeString(Integer.toString(paramInt1));
    localRILRequest.mp.writeString(Integer.toString(paramInt2));
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt1 + " " + paramInt2);
    send(localRILRequest);
  }

  public void deleteSmsOnRuim(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(97, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    send(localRILRequest);
  }

  public void deleteSmsOnSim(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(64, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    send(localRILRequest);
  }

  public void dial(String paramString, int paramInt, Message paramMessage)
  {
    dial(paramString, paramInt, null, paramMessage);
  }

  public void dial(String paramString, int paramInt, UUSInfo paramUUSInfo, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(10, paramMessage);
    localRILRequest.mp.writeString(paramString);
    localRILRequest.mp.writeInt(paramInt);
    if (paramUUSInfo == null)
    {
      riljLog("uusInfo = null");
      localRILRequest.mp.writeInt(0);
    }
    while (true)
    {
      riljLog("dial callDetails = null");
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
      send(localRILRequest);
      return;
      riljLog("uusInfo = present");
      localRILRequest.mp.writeInt(1);
      localRILRequest.mp.writeInt(paramUUSInfo.getType());
      localRILRequest.mp.writeInt(paramUUSInfo.getDcs());
      localRILRequest.mp.writeByteArray(paramUUSInfo.getUserData());
    }
  }

  public void dump(FileDescriptor paramFileDescriptor, PrintWriter paramPrintWriter, String[] paramArrayOfString)
  {
    paramPrintWriter.println("RIL:");
    paramPrintWriter.println(" mSocket=" + this.mSocket);
    paramPrintWriter.println(" mSenderThread=" + this.mSenderThread);
    paramPrintWriter.println(" mSender=" + this.mSender);
    paramPrintWriter.println(" mReceiverThread=" + this.mReceiverThread);
    paramPrintWriter.println(" mReceiver=" + this.mReceiver);
    paramPrintWriter.println(" mWakeLock=" + this.mWakeLock);
    paramPrintWriter.println(" mWakeLockTimeout=" + this.mWakeLockTimeout);
    synchronized (this.mRequestsList)
    {
      paramPrintWriter.println(" mRequestMessagesPending=" + this.mRequestMessagesPending);
      paramPrintWriter.println(" mRequestMessagesWaiting=" + this.mRequestMessagesWaiting);
      int i = this.mRequestsList.size();
      paramPrintWriter.println(" mRequestList count=" + i);
      for (int j = 0; j < i; j++)
      {
        RILRequest localRILRequest = (RILRequest)this.mRequestsList.get(j);
        paramPrintWriter.println("  [" + localRILRequest.mSerial + "] " + requestToString(localRILRequest.mRequest));
      }
      paramPrintWriter.println(" mLastNITZTimeInfo=" + this.mLastNITZTimeInfo);
      paramPrintWriter.println(" mTestingEmergencyCall=" + this.mTestingEmergencyCall.get());
      return;
    }
  }

  public void exitEmergencyCallbackMode(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(99, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void explicitCallTransfer(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(72, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getAvailableNetworks(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(48, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getBasebandVersion(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(51, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getCDMASubscription(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(95, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getCLIR(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(31, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getCdmaBroadcastConfig(Message paramMessage)
  {
    send(RILRequest.obtain(92, paramMessage));
  }

  public void getCdmaSubscriptionSource(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(104, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getCurrentCalls(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(9, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getDataCallList(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(57, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getDataCallProfile(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(506, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramInt);
    send(localRILRequest);
  }

  public void getDataRegistrationState(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(21, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getDeviceIdentity(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(98, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getGsmBroadcastConfig(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(89, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getIMEI(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(38, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getIMEISV(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(39, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getIMSI(Message paramMessage)
  {
    getIMSIForApp(null, paramMessage);
  }

  public void getIMSIForApp(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(11, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeString(paramString);
    riljLog(localRILRequest.serialString() + "> getIMSI: " + requestToString(localRILRequest.mRequest) + " aid: " + paramString);
    send(localRILRequest);
  }

  public void getIccCardStatus(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(1, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getImsRegistrationState(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(504, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getLastCallFailCause(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(18, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getLastDataCallFailCause(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(56, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getLastPdpFailCause(Message paramMessage)
  {
    getLastDataCallFailCause(paramMessage);
  }

  public void getMute(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(54, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getNeighboringCids(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(75, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getNetworkSelectionMode(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(45, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getOperator(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(22, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  @Deprecated
  public void getPDPContextList(Message paramMessage)
  {
    getDataCallList(paramMessage);
  }

  public void getPreferredNetworkType(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(74, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getPreferredVoicePrivacy(Message paramMessage)
  {
    send(RILRequest.obtain(83, paramMessage));
  }

  public void getQosStatus(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(515, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " qosId:" + paramInt + " (0x" + Integer.toHexString(paramInt) + ")");
    send(localRILRequest);
  }

  public void getSignalStrength(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(19, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getSmscAddress(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(100, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getVoiceRadioTechnology(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(108, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void getVoiceRegistrationState(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(20, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void handleCallSetupRequestFromSim(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(71, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    int[] arrayOfInt = new int[i];
    if (paramBoolean);
    while (true)
    {
      arrayOfInt[0] = i;
      localRILRequest.mp.writeIntArray(arrayOfInt);
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  public void hangupConnection(int paramInt, Message paramMessage)
  {
    riljLog("hangupConnection: gsmIndex=" + paramInt);
    RILRequest localRILRequest = RILRequest.obtain(12, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    send(localRILRequest);
  }

  public void hangupForegroundResumeBackground(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(14, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void hangupWaitingOrBackground(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(13, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void iccCloseChannel(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(525, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> iccCloseChannel: " + requestToString(localRILRequest.mRequest) + " " + paramInt);
    send(localRILRequest);
  }

  public void iccExchangeAPDU(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, String paramString, Message paramMessage)
  {
    if (paramInt3 == 0);
    for (RILRequest localRILRequest = RILRequest.obtain(523, paramMessage); ; localRILRequest = RILRequest.obtain(526, paramMessage))
    {
      localRILRequest.mp.writeInt(paramInt1);
      localRILRequest.mp.writeInt(paramInt2);
      localRILRequest.mp.writeInt(paramInt3);
      localRILRequest.mp.writeString(null);
      localRILRequest.mp.writeInt(paramInt4);
      localRILRequest.mp.writeInt(paramInt5);
      localRILRequest.mp.writeInt(paramInt6);
      localRILRequest.mp.writeString(paramString);
      localRILRequest.mp.writeString(null);
      riljLog(localRILRequest.serialString() + "> iccExchangeAPDU: " + requestToString(localRILRequest.mRequest) + " 0x" + Integer.toHexString(paramInt1) + " 0x" + Integer.toHexString(paramInt2) + " 0x" + Integer.toHexString(paramInt3) + " " + paramInt4 + "," + paramInt5 + "," + paramInt6);
      send(localRILRequest);
      return;
    }
  }

  public void iccIO(int paramInt1, int paramInt2, String paramString1, int paramInt3, int paramInt4, int paramInt5, String paramString2, String paramString3, Message paramMessage)
  {
    iccIOForApp(paramInt1, paramInt2, paramString1, paramInt3, paramInt4, paramInt5, paramString2, paramString3, null, paramMessage);
  }

  public void iccIOForApp(int paramInt1, int paramInt2, String paramString1, int paramInt3, int paramInt4, int paramInt5, String paramString2, String paramString3, String paramString4, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(28, paramMessage);
    localRILRequest.mp.writeInt(paramInt1);
    localRILRequest.mp.writeInt(paramInt2);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeInt(paramInt3);
    localRILRequest.mp.writeInt(paramInt4);
    localRILRequest.mp.writeInt(paramInt5);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    localRILRequest.mp.writeString(paramString4);
    riljLog(localRILRequest.serialString() + "> iccIO: " + requestToString(localRILRequest.mRequest) + " 0x" + Integer.toHexString(paramInt1) + " 0x" + Integer.toHexString(paramInt2) + " " + " path: " + paramString1 + "," + paramInt3 + "," + paramInt4 + "," + paramInt5 + " aid: " + paramString4);
    send(localRILRequest);
  }

  public void iccOpenChannel(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(524, paramMessage);
    localRILRequest.mp.writeString(paramString);
    riljLog(localRILRequest.serialString() + "> iccOpenChannel: " + requestToString(localRILRequest.mRequest) + " " + paramString);
    send(localRILRequest);
  }

  public void invokeDepersonalization(String paramString, int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(8, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " Type:" + paramInt);
    localRILRequest.mp.writeInt(paramInt);
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void invokeOemRilRequestRaw(byte[] paramArrayOfByte, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(59, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + "[" + IccUtils.bytesToHexString(paramArrayOfByte) + "]");
    localRILRequest.mp.writeByteArray(paramArrayOfByte);
    send(localRILRequest);
  }

  public void invokeOemRilRequestStrings(String[] paramArrayOfString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(60, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeStringArray(paramArrayOfString);
    send(localRILRequest);
  }

  public void invokeSimlessHW(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(503, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void modifyCallConfirm(Message paramMessage, CallModify paramCallModify)
  {
    RILRequest localRILRequest = RILRequest.obtain(520, paramMessage);
    setCallModify(localRILRequest, paramCallModify);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + paramCallModify);
    send(localRILRequest);
  }

  public void modifyCallInitiate(Message paramMessage, CallModify paramCallModify)
  {
    RILRequest localRILRequest = RILRequest.obtain(519, paramMessage);
    setCallModify(localRILRequest, paramCallModify);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + paramCallModify);
    send(localRILRequest);
  }

  public void modifyQos(int paramInt, ArrayList<String> paramArrayList, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(516, paramMessage);
    int i = paramArrayList.size();
    localRILRequest.mp.writeInt(i + 1);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    for (String str : (String[])paramArrayList.toArray(new String[0]))
      localRILRequest.mp.writeString(str);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " qosId:" + paramInt + " (0x" + Integer.toHexString(paramInt) + "), " + paramArrayList);
    send(localRILRequest);
  }

  public void notifySimStatusChanged()
  {
    if (this.mIccStatusChangedRegistrants != null)
    {
      Log.d("RILJ", "notifySimStatusChanged!!!");
      this.mIccStatusChangedRegistrants.notifyRegistrants();
    }
  }

  protected void onRadioAvailable()
  {
    boolean bool = true;
    if (this.mContext != null)
      bool = ((PowerManager)this.mContext.getSystemService("power")).isScreenOn();
    sendScreenState(bool);
  }

  public void queryAvailableBandMode(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(66, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void queryCLIP(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(55, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void queryCallForwardStatus(int paramInt1, int paramInt2, String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(33, paramMessage);
    localRILRequest.mp.writeInt(2);
    localRILRequest.mp.writeInt(paramInt1);
    localRILRequest.mp.writeInt(paramInt2);
    localRILRequest.mp.writeInt(PhoneNumberUtils.toaFromString(paramString));
    localRILRequest.mp.writeString(paramString);
    localRILRequest.mp.writeInt(0);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt1 + " " + paramInt2);
    send(localRILRequest);
  }

  public void queryCallWaiting(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(35, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt);
    send(localRILRequest);
  }

  public void queryCdmaRoamingPreference(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(79, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void queryFacilityLock(String paramString1, String paramString2, int paramInt, Message paramMessage)
  {
    queryFacilityLockForApp(paramString1, paramString2, paramInt, null, paramMessage);
  }

  public void queryFacilityLockForApp(String paramString1, String paramString2, int paramInt, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(42, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " [" + paramString1 + " " + paramInt + " " + paramString3 + "]");
    localRILRequest.mp.writeInt(4);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    localRILRequest.mp.writeString(paramString3);
    send(localRILRequest);
  }

  public void queryTTYMode(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(81, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void rejectCall(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(17, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void releaseQos(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(514, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " qosId:" + paramInt + " (0x" + Integer.toHexString(paramInt) + ")");
    send(localRILRequest);
  }

  public void reportSmsMemoryStatus(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(102, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    while (true)
    {
      localParcel.writeInt(i);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + ": " + paramBoolean);
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  public void reportStkServiceIsRunning(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(103, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void requestIsimAuthentication(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(105, paramMessage);
    localRILRequest.mp.writeString(paramString);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void requestSetEmergencyNumbers(String paramString1, String paramString2)
  {
    riljLog("setEmergencyNumbers()");
    RILRequest localRILRequest = RILRequest.obtain(501, null);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(2);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    send(localRILRequest);
  }

  public void resetAllConnections()
  {
    RILRequest localRILRequest = RILRequest.obtain(536, null);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void resetRadio(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(58, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void restartRild(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(521, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void resumeQos(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(518, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " qosId:" + paramInt + " (0x" + Integer.toHexString(paramInt) + ")");
    send(localRILRequest);
  }

  public void sendBurstDtmf(String paramString, int paramInt1, int paramInt2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(85, paramMessage);
    localRILRequest.mp.writeInt(3);
    localRILRequest.mp.writeString(paramString);
    localRILRequest.mp.writeString(Integer.toString(paramInt1));
    localRILRequest.mp.writeString(Integer.toString(paramInt2));
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramString);
    send(localRILRequest);
  }

  public void sendCDMAFeatureCode(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(84, paramMessage);
    localRILRequest.mp.writeString(paramString);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramString);
    send(localRILRequest);
  }

  public void sendCdmaSms(byte[] paramArrayOfByte, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(87, paramMessage);
    constructCdmaSendSmsRilRequest(localRILRequest, paramArrayOfByte);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void sendDtmf(char paramChar, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(24, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeString(Character.toString(paramChar));
    send(localRILRequest);
  }

  public void sendEnvelope(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(69, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void sendEnvelopeWithStatus(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(107, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + '[' + paramString + ']');
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void sendImsCdmaSms(byte[] paramArrayOfByte, int paramInt1, int paramInt2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(505, paramMessage);
    localRILRequest.mp.writeInt(2);
    localRILRequest.mp.writeByte((byte)paramInt1);
    localRILRequest.mp.writeInt(paramInt2);
    constructCdmaSendSmsRilRequest(localRILRequest, paramArrayOfByte);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void sendImsGsmSms(String paramString1, String paramString2, int paramInt1, int paramInt2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(505, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeByte((byte)paramInt1);
    localRILRequest.mp.writeInt(paramInt2);
    constructGsmSendSmsRilRequest(localRILRequest, paramString1, paramString2);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void sendSMS(String paramString1, String paramString2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(25, paramMessage);
    constructGsmSendSmsRilRequest(localRILRequest, paramString1, paramString2);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void sendTerminalResponse(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(70, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void sendUSSD(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(29, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramString);
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void separateConnection(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(52, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    send(localRILRequest);
  }

  public void setBandMode(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(65, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt);
    send(localRILRequest);
  }

  public void setCLIR(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(32, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt);
    send(localRILRequest);
  }

  public void setCallForward(int paramInt1, int paramInt2, int paramInt3, String paramString, int paramInt4, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(34, paramMessage);
    localRILRequest.mp.writeInt(paramInt1);
    localRILRequest.mp.writeInt(paramInt2);
    localRILRequest.mp.writeInt(paramInt3);
    localRILRequest.mp.writeInt(PhoneNumberUtils.toaFromString(paramString));
    localRILRequest.mp.writeString(paramString);
    localRILRequest.mp.writeInt(paramInt4);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramInt1 + " " + paramInt2 + " " + paramInt3 + paramInt4);
    send(localRILRequest);
  }

  public void setCallWaiting(boolean paramBoolean, int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(36, paramMessage);
    localRILRequest.mp.writeInt(2);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    for (int i = 1; ; i = 0)
    {
      localParcel.writeInt(i);
      localRILRequest.mp.writeInt(paramInt);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramBoolean + ", " + paramInt);
      send(localRILRequest);
      return;
    }
  }

  public void setCdmaBroadcastActivation(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(94, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean)
      i = 0;
    localParcel.writeInt(i);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void setCdmaBroadcastConfig(int[] paramArrayOfInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(93, paramMessage);
    for (int i = 0; i < paramArrayOfInt.length; i++)
      localRILRequest.mp.writeInt(paramArrayOfInt[i]);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] paramArrayOfCdmaSmsBroadcastConfigInfo, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(93, paramMessage);
    ArrayList localArrayList = new ArrayList();
    int i = paramArrayOfCdmaSmsBroadcastConfigInfo.length;
    for (int j = 0; j < i; j++)
    {
      CdmaSmsBroadcastConfigInfo localCdmaSmsBroadcastConfigInfo = paramArrayOfCdmaSmsBroadcastConfigInfo[j];
      for (int i1 = localCdmaSmsBroadcastConfigInfo.getFromServiceCategory(); i1 <= localCdmaSmsBroadcastConfigInfo.getToServiceCategory(); i1++)
        localArrayList.add(new CdmaSmsBroadcastConfigInfo(i1, i1, localCdmaSmsBroadcastConfigInfo.getLanguage(), localCdmaSmsBroadcastConfigInfo.isSelected()));
    }
    CdmaSmsBroadcastConfigInfo[] arrayOfCdmaSmsBroadcastConfigInfo = (CdmaSmsBroadcastConfigInfo[])localArrayList.toArray(paramArrayOfCdmaSmsBroadcastConfigInfo);
    localRILRequest.mp.writeInt(arrayOfCdmaSmsBroadcastConfigInfo.length);
    int k = 0;
    if (k < arrayOfCdmaSmsBroadcastConfigInfo.length)
    {
      localRILRequest.mp.writeInt(arrayOfCdmaSmsBroadcastConfigInfo[k].getFromServiceCategory());
      localRILRequest.mp.writeInt(arrayOfCdmaSmsBroadcastConfigInfo[k].getLanguage());
      Parcel localParcel = localRILRequest.mp;
      if (arrayOfCdmaSmsBroadcastConfigInfo[k].isSelected());
      for (int n = 1; ; n = 0)
      {
        localParcel.writeInt(n);
        k++;
        break;
      }
    }
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " with " + arrayOfCdmaSmsBroadcastConfigInfo.length + "configs : ");
    for (int m = 0; m < arrayOfCdmaSmsBroadcastConfigInfo.length; m++)
      riljLog(arrayOfCdmaSmsBroadcastConfigInfo[m].toString());
    send(localRILRequest);
  }

  public void setCdmaRoamingPreference(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(78, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramInt);
    send(localRILRequest);
  }

  public void setCdmaSubscriptionSource(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(77, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramInt);
    send(localRILRequest);
  }

  public void setCurrentPreferredNetworkType()
  {
    riljLog("setCurrentPreferredNetworkType: " + this.mSetPreferredNetworkType);
    setPreferredNetworkType(this.mSetPreferredNetworkType, null);
  }

  public void setDataSubscription(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(508, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void setFacilityLock(String paramString1, boolean paramBoolean, String paramString2, int paramInt, Message paramMessage)
  {
    setFacilityLockForApp(paramString1, paramBoolean, paramString2, paramInt, null, paramMessage);
  }

  public void setFacilityLockForApp(String paramString1, boolean paramBoolean, String paramString2, int paramInt, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(43, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " [" + paramString1 + " " + paramBoolean + " " + paramInt + " " + paramString3 + "]");
    localRILRequest.mp.writeInt(5);
    localRILRequest.mp.writeString(paramString1);
    if (paramBoolean);
    for (String str = "1"; ; str = "0")
    {
      localRILRequest.mp.writeString(str);
      localRILRequest.mp.writeString(paramString2);
      localRILRequest.mp.writeString(Integer.toString(paramInt));
      localRILRequest.mp.writeString(paramString3);
      send(localRILRequest);
      return;
    }
  }

  public void setGsmBroadcastActivation(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(91, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean)
      i = 0;
    localParcel.writeInt(i);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] paramArrayOfSmsBroadcastConfigInfo, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(90, paramMessage);
    int i = paramArrayOfSmsBroadcastConfigInfo.length;
    localRILRequest.mp.writeInt(i);
    int j = 0;
    if (j < i)
    {
      localRILRequest.mp.writeInt(paramArrayOfSmsBroadcastConfigInfo[j].getFromServiceId());
      localRILRequest.mp.writeInt(paramArrayOfSmsBroadcastConfigInfo[j].getToServiceId());
      localRILRequest.mp.writeInt(paramArrayOfSmsBroadcastConfigInfo[j].getFromCodeScheme());
      localRILRequest.mp.writeInt(paramArrayOfSmsBroadcastConfigInfo[j].getToCodeScheme());
      Parcel localParcel = localRILRequest.mp;
      if (paramArrayOfSmsBroadcastConfigInfo[j].isSelected());
      for (int m = 1; ; m = 0)
      {
        localParcel.writeInt(m);
        j++;
        break;
      }
    }
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " with " + i + " configs : ");
    for (int k = 0; k < i; k++)
      riljLog(paramArrayOfSmsBroadcastConfigInfo[k].toString());
    send(localRILRequest);
  }

  public void setLocationUpdates(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(76, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    while (true)
    {
      localParcel.writeInt(i);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + ": " + paramBoolean);
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  public void setModemPower(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(502, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    StringBuilder localStringBuilder;
    if (paramBoolean)
    {
      localParcel.writeInt(i);
      localStringBuilder = new StringBuilder().append(localRILRequest.serialString()).append("> ").append(requestToString(localRILRequest.mRequest));
      if (!paramBoolean)
        break label106;
    }
    label106: for (String str = " on"; ; str = " off")
    {
      riljLog(str);
      send(localRILRequest);
      return;
      i = 0;
      break;
    }
  }

  public void setMute(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(53, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramBoolean);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    while (true)
    {
      localParcel.writeInt(i);
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  public void setNetworkSelectionModeAutomatic(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(46, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void setNetworkSelectionModeManual(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(47, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramString);
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void setOnNITZTime(Handler paramHandler, int paramInt, Object paramObject)
  {
    super.setOnNITZTime(paramHandler, paramInt, paramObject);
    if (this.mLastNITZTimeInfo != null)
    {
      this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult(null, this.mLastNITZTimeInfo, null));
      this.mLastNITZTimeInfo = null;
    }
  }

  public void setPhoneType(int paramInt)
  {
    riljLog("setPhoneType=" + paramInt + " old value=" + this.mPhoneType);
    this.mPhoneType = paramInt;
  }

  public void setPreferredNetworkType(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(73, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    this.mSetPreferredNetworkType = paramInt;
    this.mPreferredNetworkType = paramInt;
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramInt);
    send(localRILRequest);
  }

  public void setPreferredVoicePrivacy(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(82, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    while (true)
    {
      localParcel.writeInt(i);
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  public void setRadioPower(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(23, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    StringBuilder localStringBuilder;
    if (paramBoolean)
    {
      localParcel.writeInt(i);
      localStringBuilder = new StringBuilder().append(localRILRequest.serialString()).append("> ").append(requestToString(localRILRequest.mRequest));
      if (!paramBoolean)
        break label105;
    }
    label105: for (String str = " on"; ; str = " off")
    {
      riljLog(str);
      send(localRILRequest);
      return;
      i = 0;
      break;
    }
  }

  public void setSmscAddress(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(101, paramMessage);
    localRILRequest.mp.writeString(paramString);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramString);
    send(localRILRequest);
  }

  public void setSubscriptionMode(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(511, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " subscriptionMode: " + paramInt);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    send(localRILRequest);
  }

  public void setSuppServiceNotifications(boolean paramBoolean, Message paramMessage)
  {
    int i = 1;
    RILRequest localRILRequest = RILRequest.obtain(62, paramMessage);
    localRILRequest.mp.writeInt(i);
    Parcel localParcel = localRILRequest.mp;
    if (paramBoolean);
    while (true)
    {
      localParcel.writeInt(i);
      riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
      send(localRILRequest);
      return;
      i = 0;
    }
  }

  public void setTTYMode(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(80, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramInt);
    send(localRILRequest);
  }

  public void setUiccSubscription(int paramInt1, int paramInt2, int paramInt3, int paramInt4, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(507, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " slot: " + paramInt1 + " appIndex: " + paramInt2 + " subId: " + paramInt3 + " subStatus: " + paramInt4);
    localRILRequest.mp.writeInt(paramInt1);
    localRILRequest.mp.writeInt(paramInt2);
    localRILRequest.mp.writeInt(paramInt3);
    localRILRequest.mp.writeInt(paramInt4);
    send(localRILRequest);
  }

  public void setupDataCall(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6, String paramString7, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(27, paramMessage);
    localRILRequest.mp.writeInt(7);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    localRILRequest.mp.writeString(paramString4);
    localRILRequest.mp.writeString(paramString5);
    localRILRequest.mp.writeString(paramString6);
    localRILRequest.mp.writeString(paramString7);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " " + paramString1 + " " + paramString2 + " " + paramString3 + " " + paramString4 + " " + paramString5 + " " + paramString6 + " " + paramString7);
    send(localRILRequest);
  }

  public void setupQosReq(int paramInt, ArrayList<String> paramArrayList, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(513, paramMessage);
    int i = paramArrayList.size();
    localRILRequest.mp.writeInt(i + 1);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    for (String str : (String[])paramArrayList.toArray(new String[0]))
      localRILRequest.mp.writeString(str);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " callId:" + paramInt + ", " + paramArrayList);
    send(localRILRequest);
  }

  public void startDtmf(char paramChar, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(49, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeString(Character.toString(paramChar));
    send(localRILRequest);
  }

  public void stopDtmf(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(50, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void supplyDepersonalization(String paramString, int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(8, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " Type:" + paramInt);
    localRILRequest.mp.writeInt(paramInt);
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void supplyIccPin(String paramString, Message paramMessage)
  {
    supplyIccPinForApp(paramString, null, paramMessage);
  }

  public void supplyIccPin2(String paramString, Message paramMessage)
  {
    supplyIccPin2ForApp(paramString, null, paramMessage);
  }

  public void supplyIccPin2ForApp(String paramString1, String paramString2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(4, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(2);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    send(localRILRequest);
  }

  public void supplyIccPinForApp(String paramString1, String paramString2, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(2, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(2);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    send(localRILRequest);
  }

  public void supplyIccPuk(String paramString1, String paramString2, Message paramMessage)
  {
    supplyIccPukForApp(paramString1, paramString2, null, paramMessage);
  }

  public void supplyIccPuk2(String paramString1, String paramString2, Message paramMessage)
  {
    supplyIccPuk2ForApp(paramString1, paramString2, null, paramMessage);
  }

  public void supplyIccPuk2ForApp(String paramString1, String paramString2, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(5, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(3);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    send(localRILRequest);
  }

  public void supplyIccPukForApp(String paramString1, String paramString2, String paramString3, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(3, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(3);
    localRILRequest.mp.writeString(paramString1);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString3);
    send(localRILRequest);
  }

  public void supplyNetworkDepersonalization(String paramString, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(8, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeString(paramString);
    send(localRILRequest);
  }

  public void suspendQos(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(517, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeString(Integer.toString(paramInt));
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " qosId:" + paramInt + " (0x" + Integer.toHexString(paramInt) + ")");
    send(localRILRequest);
  }

  public void switchMtkSim(int paramInt, Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(522, paramMessage);
    localRILRequest.mp.writeInt(1);
    localRILRequest.mp.writeInt(paramInt);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest) + " : " + paramInt);
    send(localRILRequest);
  }

  public void switchWaitingOrHoldingAndActive(Message paramMessage)
  {
    RILRequest localRILRequest = RILRequest.obtain(15, paramMessage);
    riljLog(localRILRequest.serialString() + "> " + requestToString(localRILRequest.mRequest));
    send(localRILRequest);
  }

  public void testingEmergencyCall()
  {
    riljLog("testingEmergencyCall");
    this.mTestingEmergencyCall.set(true);
  }

  public void writeSmsToRuim(int paramInt, String paramString, Message paramMessage)
  {
    int i = translateStatus(paramInt);
    RILRequest localRILRequest = RILRequest.obtain(96, paramMessage);
    localRILRequest.mp.writeInt(i);
    this.mRILEx.writeContent(localRILRequest, paramString);
    send(localRILRequest);
  }

  public void writeSmsToSim(int paramInt, String paramString1, String paramString2, Message paramMessage)
  {
    int i = translateStatus(paramInt);
    RILRequest localRILRequest = RILRequest.obtain(63, paramMessage);
    localRILRequest.mp.writeInt(i);
    localRILRequest.mp.writeString(paramString2);
    localRILRequest.mp.writeString(paramString1);
    send(localRILRequest);
  }

  private class RILEx
  {
    private RILEx()
    {
    }

    private void writeContent(RILRequest paramRILRequest, String paramString)
    {
      try
      {
        for (int k : paramString.getBytes("ISO-8859-1"))
          Log.e("RILJ", "writeSmsToRuim pdu is" + k);
        DataInputStream localDataInputStream = new DataInputStream(new ByteArrayInputStream(paramString.getBytes("ISO-8859-1")));
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
        for (int m = 0; m < 36; m++)
          paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
        paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
        for (int n = 0; n < 36; n++)
          paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
        paramRILRequest.mp.writeInt(localDataInputStream.readInt());
        for (int i1 = 0; i1 < 255; i1++)
          paramRILRequest.mp.writeByte((byte)localDataInputStream.read());
      }
      catch (UnsupportedEncodingException localUnsupportedEncodingException)
      {
        RIL.this.riljLog("writeSmsToRuim: UnsupportedEncodingException: " + localUnsupportedEncodingException);
        return;
      }
      catch (IOException localIOException)
      {
        RIL.this.riljLog("writeSmsToRuim: conversion from input stream to object failed: " + localIOException);
      }
    }
  }

  class RILReceiver
    implements Runnable
  {
    byte[] buffer = new byte[8192];

    RILReceiver()
    {
    }

    // ERROR //
    public void run()
    {
      // Byte code:
      //   0: iconst_0
      //   1: istore_1
      //   2: aconst_null
      //   3: astore_2
      //   4: ldc 28
      //   6: iconst_0
      //   7: invokestatic 34	android/os/SystemProperties:getBoolean	(Ljava/lang/String;Z)Z
      //   10: istore 5
      //   12: aload_0
      //   13: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   16: invokestatic 40	com/android/internal/telephony/RIL:access$300	(Lcom/android/internal/telephony/RIL;)Ljava/lang/Integer;
      //   19: ifnull +21 -> 40
      //   22: aload_0
      //   23: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   26: invokestatic 40	com/android/internal/telephony/RIL:access$300	(Lcom/android/internal/telephony/RIL;)Ljava/lang/Integer;
      //   29: invokevirtual 46	java/lang/Integer:intValue	()I
      //   32: ifeq +8 -> 40
      //   35: iload 5
      //   37: ifne +203 -> 240
      //   40: ldc 48
      //   42: astore 6
      //   44: new 50	android/net/LocalSocket
      //   47: dup
      //   48: invokespecial 51	android/net/LocalSocket:<init>	()V
      //   51: astore 7
      //   53: aload 7
      //   55: new 53	android/net/LocalSocketAddress
      //   58: dup
      //   59: aload 6
      //   61: getstatic 59	android/net/LocalSocketAddress$Namespace:RESERVED	Landroid/net/LocalSocketAddress$Namespace;
      //   64: invokespecial 62	android/net/LocalSocketAddress:<init>	(Ljava/lang/String;Landroid/net/LocalSocketAddress$Namespace;)V
      //   67: invokevirtual 66	android/net/LocalSocket:connect	(Landroid/net/LocalSocketAddress;)V
      //   70: aload_0
      //   71: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   74: aload 7
      //   76: putfield 70	com/android/internal/telephony/RIL:mSocket	Landroid/net/LocalSocket;
      //   79: ldc 72
      //   81: new 74	java/lang/StringBuilder
      //   84: dup
      //   85: invokespecial 75	java/lang/StringBuilder:<init>	()V
      //   88: ldc 77
      //   90: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   93: aload 6
      //   95: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   98: ldc 83
      //   100: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   103: invokevirtual 87	java/lang/StringBuilder:toString	()Ljava/lang/String;
      //   106: invokestatic 93	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
      //   109: pop
      //   110: iconst_0
      //   111: istore 14
      //   113: aload_0
      //   114: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   117: getfield 70	com/android/internal/telephony/RIL:mSocket	Landroid/net/LocalSocket;
      //   120: invokevirtual 97	android/net/LocalSocket:getInputStream	()Ljava/io/InputStream;
      //   123: astore 21
      //   125: aload 21
      //   127: aload_0
      //   128: getfield 19	com/android/internal/telephony/RIL$RILReceiver:buffer	[B
      //   131: invokestatic 101	com/android/internal/telephony/RIL:access$400	(Ljava/io/InputStream;[B)I
      //   134: istore 22
      //   136: iload 22
      //   138: istore 14
      //   140: iload 14
      //   142: ifge +221 -> 363
      //   145: ldc 72
      //   147: new 74	java/lang/StringBuilder
      //   150: dup
      //   151: invokespecial 75	java/lang/StringBuilder:<init>	()V
      //   154: ldc 103
      //   156: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   159: aload 6
      //   161: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   164: ldc 83
      //   166: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   169: invokevirtual 87	java/lang/StringBuilder:toString	()Ljava/lang/String;
      //   172: invokestatic 93	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
      //   175: pop
      //   176: aload_0
      //   177: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   180: getstatic 109	com/android/internal/telephony/CommandsInterface$RadioState:RADIO_UNAVAILABLE	Lcom/android/internal/telephony/CommandsInterface$RadioState;
      //   183: invokevirtual 113	com/android/internal/telephony/RIL:setRadioState	(Lcom/android/internal/telephony/CommandsInterface$RadioState;)V
      //   186: aload_0
      //   187: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   190: getfield 70	com/android/internal/telephony/RIL:mSocket	Landroid/net/LocalSocket;
      //   193: invokevirtual 116	android/net/LocalSocket:close	()V
      //   196: aload_0
      //   197: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   200: aconst_null
      //   201: putfield 70	com/android/internal/telephony/RIL:mSocket	Landroid/net/LocalSocket;
      //   204: invokestatic 121	com/android/internal/telephony/RILRequest:resetSerial	()V
      //   207: aload_0
      //   208: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   211: iconst_1
      //   212: iconst_0
      //   213: invokestatic 125	com/android/internal/telephony/RIL:access$600	(Lcom/android/internal/telephony/RIL;IZ)V
      //   216: iconst_0
      //   217: istore_1
      //   218: goto -216 -> 2
      //   221: astore_3
      //   222: ldc 72
      //   224: ldc 127
      //   226: aload_3
      //   227: invokestatic 131	android/util/Log:e	(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
      //   230: pop
      //   231: aload_0
      //   232: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   235: iconst_m1
      //   236: invokestatic 135	com/android/internal/telephony/RIL:access$700	(Lcom/android/internal/telephony/RIL;I)V
      //   239: return
      //   240: ldc 137
      //   242: astore 6
      //   244: goto -200 -> 44
      //   247: astore 24
      //   249: aload_2
      //   250: ifnull +7 -> 257
      //   253: aload_2
      //   254: invokevirtual 116	android/net/LocalSocket:close	()V
      //   257: iload_1
      //   258: bipush 8
      //   260: if_icmpne +55 -> 315
      //   263: ldc 72
      //   265: new 74	java/lang/StringBuilder
      //   268: dup
      //   269: invokespecial 75	java/lang/StringBuilder:<init>	()V
      //   272: ldc 139
      //   274: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   277: aload 6
      //   279: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   282: ldc 141
      //   284: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   287: iload_1
      //   288: invokevirtual 144	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
      //   291: ldc 146
      //   293: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   296: invokevirtual 87	java/lang/StringBuilder:toString	()Ljava/lang/String;
      //   299: invokestatic 148	android/util/Log:e	(Ljava/lang/String;Ljava/lang/String;)I
      //   302: pop
      //   303: ldc2_w 149
      //   306: invokestatic 156	java/lang/Thread:sleep	(J)V
      //   309: iinc 1 1
      //   312: goto -310 -> 2
      //   315: iload_1
      //   316: ifle -13 -> 303
      //   319: iload_1
      //   320: bipush 8
      //   322: if_icmpge -19 -> 303
      //   325: ldc 72
      //   327: new 74	java/lang/StringBuilder
      //   330: dup
      //   331: invokespecial 75	java/lang/StringBuilder:<init>	()V
      //   334: ldc 139
      //   336: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   339: aload 6
      //   341: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   344: ldc 158
      //   346: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   349: invokevirtual 87	java/lang/StringBuilder:toString	()Ljava/lang/String;
      //   352: invokestatic 93	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
      //   355: pop
      //   356: goto -53 -> 303
      //   359: astore_3
      //   360: goto -138 -> 222
      //   363: invokestatic 164	android/os/Parcel:obtain	()Landroid/os/Parcel;
      //   366: astore 23
      //   368: aload 23
      //   370: aload_0
      //   371: getfield 19	com/android/internal/telephony/RIL$RILReceiver:buffer	[B
      //   374: iconst_0
      //   375: iload 14
      //   377: invokevirtual 168	android/os/Parcel:unmarshall	([BII)V
      //   380: aload 23
      //   382: iconst_0
      //   383: invokevirtual 172	android/os/Parcel:setDataPosition	(I)V
      //   386: aload_0
      //   387: getfield 14	com/android/internal/telephony/RIL$RILReceiver:this$0	Lcom/android/internal/telephony/RIL;
      //   390: aload 23
      //   392: invokestatic 176	com/android/internal/telephony/RIL:access$500	(Lcom/android/internal/telephony/RIL;Landroid/os/Parcel;)V
      //   395: aload 23
      //   397: invokevirtual 179	android/os/Parcel:recycle	()V
      //   400: goto -275 -> 125
      //   403: astore 19
      //   405: ldc 72
      //   407: new 74	java/lang/StringBuilder
      //   410: dup
      //   411: invokespecial 75	java/lang/StringBuilder:<init>	()V
      //   414: ldc 181
      //   416: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   419: aload 6
      //   421: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   424: ldc 183
      //   426: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   429: invokevirtual 87	java/lang/StringBuilder:toString	()Ljava/lang/String;
      //   432: aload 19
      //   434: invokestatic 185	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
      //   437: pop
      //   438: goto -293 -> 145
      //   441: astore 15
      //   443: ldc 72
      //   445: new 74	java/lang/StringBuilder
      //   448: dup
      //   449: invokespecial 75	java/lang/StringBuilder:<init>	()V
      //   452: ldc 187
      //   454: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   457: iload 14
      //   459: invokevirtual 144	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
      //   462: ldc 189
      //   464: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   467: aload 15
      //   469: invokevirtual 190	java/lang/Throwable:toString	()Ljava/lang/String;
      //   472: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   475: invokevirtual 87	java/lang/StringBuilder:toString	()Ljava/lang/String;
      //   478: invokestatic 148	android/util/Log:e	(Ljava/lang/String;Ljava/lang/String;)I
      //   481: pop
      //   482: goto -337 -> 145
      //   485: astore 12
      //   487: goto -230 -> 257
      //   490: astore 10
      //   492: goto -183 -> 309
      //   495: astore 18
      //   497: goto -301 -> 196
      //   500: astore 8
      //   502: aload 7
      //   504: astore_2
      //   505: goto -256 -> 249
      //
      // Exception table:
      //   from	to	target	type
      //   53	70	221	java/lang/Throwable
      //   70	110	221	java/lang/Throwable
      //   145	186	221	java/lang/Throwable
      //   186	196	221	java/lang/Throwable
      //   196	216	221	java/lang/Throwable
      //   405	438	221	java/lang/Throwable
      //   443	482	221	java/lang/Throwable
      //   44	53	247	java/io/IOException
      //   4	35	359	java/lang/Throwable
      //   44	53	359	java/lang/Throwable
      //   253	257	359	java/lang/Throwable
      //   263	303	359	java/lang/Throwable
      //   303	309	359	java/lang/Throwable
      //   325	356	359	java/lang/Throwable
      //   113	125	403	java/io/IOException
      //   125	136	403	java/io/IOException
      //   363	400	403	java/io/IOException
      //   113	125	441	java/lang/Throwable
      //   125	136	441	java/lang/Throwable
      //   363	400	441	java/lang/Throwable
      //   253	257	485	java/io/IOException
      //   303	309	490	java/lang/InterruptedException
      //   186	196	495	java/io/IOException
      //   53	70	500	java/io/IOException
    }
  }

  class RILSender extends Handler
    implements Runnable
  {
    byte[] dataLength = new byte[4];

    public RILSender(Looper arg2)
    {
      super();
    }

    public void handleMessage(Message paramMessage)
    {
      RILRequest localRILRequest1 = (RILRequest)paramMessage.obj;
      switch (paramMessage.what)
      {
      default:
      case 1:
        while (true)
        {
          return;
          int k = 0;
          try
          {
            localLocalSocket = RIL.this.mSocket;
            if (localLocalSocket == null)
            {
              localRILRequest1.onError(1, null);
              localRILRequest1.release();
              int m = RIL.this.mRequestMessagesPending;
              k = 0;
              if (m > 0)
              {
                RIL localRIL3 = RIL.this;
                localRIL3.mRequestMessagesPending = (-1 + localRIL3.mRequestMessagesPending);
              }
              return;
            }
          }
          catch (IOException localIOException)
          {
          }
          catch (RuntimeException localRuntimeException)
          {
            while (true)
            {
              LocalSocket localLocalSocket;
              RIL localRIL4;
              int n;
              RIL localRIL5;
              byte[] arrayOfByte1;
              RIL localRIL2;
              Log.e("RILJ", "Uncaught exception ", localRuntimeException);
              if ((RIL.this.findAndRemoveRequestFromList(localRILRequest1.mSerial) != null) || (k == 0))
              {
                localRILRequest1.onError(2, null);
                localRILRequest1.release();
              }
              RIL localRIL1 = RIL.this;
              continue;
              byte[] arrayOfByte2 = this.dataLength;
              this.dataLength[1] = 0;
              arrayOfByte2[0] = 0;
              this.dataLength[2] = ((byte)(0xFF & arrayOfByte1.length >> 8));
              this.dataLength[3] = ((byte)(0xFF & arrayOfByte1.length));
              localLocalSocket.getOutputStream().write(this.dataLength);
              localLocalSocket.getOutputStream().write(arrayOfByte1);
              localRIL1 = RIL.this;
            }
          }
          finally
          {
            RIL.this.releaseWakeLockIfDone();
          }
        }
      case 2:
      }
      synchronized (RIL.this.mWakeLock)
      {
        if (RIL.this.mWakeLock.isHeld())
          if (RIL.this.mRequestMessagesWaiting != 0)
          {
            Log.d("RILJ", "NOTE: mReqWaiting is NOT 0 but" + RIL.this.mRequestMessagesWaiting + " at TIMEOUT, reset!" + " There still msg waitng for response");
            RIL.this.mRequestMessagesWaiting = 0;
          }
      }
      synchronized (RIL.this.mRequestsList)
      {
        int i = RIL.this.mRequestsList.size();
        Log.d("RILJ", "WAKE_LOCK_TIMEOUT  mRequestList=" + i);
        for (int j = 0; j < i; j++)
        {
          RILRequest localRILRequest2 = (RILRequest)RIL.this.mRequestsList.get(j);
          Log.d("RILJ", j + ": [" + localRILRequest2.mSerial + "] " + RIL.requestToString(localRILRequest2.mRequest));
        }
        if (RIL.this.mRequestMessagesPending != 0)
        {
          Log.e("RILJ", "ERROR: mReqPending is NOT 0 but" + RIL.this.mRequestMessagesPending + " at TIMEOUT, reset!");
          RIL.this.mRequestMessagesPending = 0;
        }
        RIL.this.mWakeLock.release();
        return;
        localObject1 = finally;
        throw localObject1;
      }
    }

    public void run()
    {
    }
  }
}
