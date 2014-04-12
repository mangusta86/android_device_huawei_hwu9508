package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Parcel;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.SignalStrength;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

public class EPRJ9508RIL extends RIL
  implements CommandsInterface
{
  public EPRJ9508RIL(Context paramContext, int paramInt1, int paramInt2)
  {
    super(paramContext, paramInt1, paramInt2);
  }

  protected void processSolicited(Parcel paramParcel)
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
        case 26:
        case 27:
        case 28:
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
        case 105:
        case 106:
        case 107:
        case 108:
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
        label684: localRILRequest.onError(j, localObject2);
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
        localObject2 = responseSMS(paramParcel);
        break;
        localObject2 = responseSetupDataCall(paramParcel);
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
        localObject2 = responseString(paramParcel);
        break;
        localObject2 = responseVoid(paramParcel);
        break;
        localObject2 = responseICC_IO(paramParcel);
        break;
        Object localObject1 = responseInts(paramParcel);
        localObject2 = localObject1;
        break;
        if (this.mIccStatusChangedRegistrants == null)
          break label684;
        riljLog("ON enter sim puk fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
        this.mIccStatusChangedRegistrants.notifyRegistrants();
        break label684;
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

  protected void processUnsolicited(Parcel paramParcel)
  {
    int i = paramParcel.dataPosition();
    int j = paramParcel.readInt();
    switch (j)
    {
    default:
      paramParcel.setDataPosition(i);
      super.processUnsolicited(paramParcel);
    case 1009:
    }
    Object localObject;
    do
    {
      return;
      localObject = responseSignalStrength(paramParcel);
      switch (j)
      {
      default:
        return;
      case 1009:
      }
      riljLog("EternityProject: RIL_UNSOL_SIGNAL_STRENGTH");
      unsljLogvRet(j, localObject);
    }
    while (this.mSignalStrengthRegistrant == null);
    this.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult(null, localObject, null));
  }

  protected Object responseSignalStrength(Parcel paramParcel)
  {
    int[] arrayOfInt = new int[13];
    for (int i = 0; i <= 12; i++)
      arrayOfInt[i] = paramParcel.readInt();
    int j;
    boolean bool;
    if (arrayOfInt[0] == 0)
    {
      j = 2;
      bool = false;
      if ((arrayOfInt[j] != -1) && (arrayOfInt[j] != 0))
        break label101;
      arrayOfInt[0] = arrayOfInt[j];
    }
    while (true)
    {
      return new SignalStrength(arrayOfInt[0], arrayOfInt[1], arrayOfInt[2], arrayOfInt[3], arrayOfInt[4], arrayOfInt[5], arrayOfInt[6], bool);
      bool = true;
      j = 0;
      break;
      label101: arrayOfInt[0] = ((113 + arrayOfInt[j]) / 2);
    }
  }
}
