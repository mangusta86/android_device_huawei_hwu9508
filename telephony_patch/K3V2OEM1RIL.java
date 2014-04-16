package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Parcel;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.cdma.CdmaInformationRecords;

public class K3V2OEM1RIL extends RIL implements CommandsInterface
{
  public K3V2OEM1RIL(Context paramContext, int paramInt1, int paramInt2)
  {
    super(paramContext, paramInt1, paramInt2);
  }

  protected void
  processSolicited (Parcel p) {
      int serial, error;
      boolean found = false;

      serial = p.readInt();
      error = p.readInt();

      RILRequest rr;

      rr = findAndRemoveRequestFromList(serial);

      if (rr == null) {
          Log.w(LOG_TAG, "Unexpected solicited response! sn: "
                          + serial + " error: " + error);
          return;
      }

      Object ret = null;

      if (error == 0 || p.dataAvail() > 0) {
          // either command succeeds or command fails but with data payload
          try {switch (rr.mRequest) {
          /*
cat libs/telephony/ril_commands.h \
| egrep "^ *{RIL_" \
| sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: ret = \2(p); break;/'
           */
          case RIL_REQUEST_GET_SIM_STATUS: ret =  responseIccCardStatus(p); break;
          case RIL_REQUEST_ENTER_SIM_PIN: ret =  responseInts(p); break;
          case RIL_REQUEST_ENTER_SIM_PUK: ret =  responseInts(p); break;
          case RIL_REQUEST_ENTER_SIM_PIN2: ret =  responseInts(p); break;
          case RIL_REQUEST_ENTER_SIM_PUK2: ret =  responseInts(p); break;
          case RIL_REQUEST_CHANGE_SIM_PIN: ret =  responseInts(p); break;
          case RIL_REQUEST_CHANGE_SIM_PIN2: ret =  responseInts(p); break;
          case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION: ret =  responseInts(p); break;
          case RIL_REQUEST_GET_CURRENT_CALLS: ret =  responseCallList(p); break;
          case RIL_REQUEST_DIAL: ret =  responseVoid(p); break;
          case RIL_REQUEST_GET_IMSI: ret =  responseString(p); break;
          case RIL_REQUEST_HANGUP: ret =  responseVoid(p); break;
          case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: ret =  responseVoid(p); break;
          case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: {
              if (mTestingEmergencyCall.getAndSet(false)) {
                  if (mEmergencyCallbackModeRegistrant != null) {
                      riljLog("testing emergency call, notify ECM Registrants");
                      mEmergencyCallbackModeRegistrant.notifyRegistrant();
                  }
              }
              ret =  responseVoid(p);
              break;
          }
          case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: ret =  responseVoid(p); break;
          case RIL_REQUEST_CONFERENCE: ret =  responseVoid(p); break;
          case RIL_REQUEST_UDUB: ret =  responseVoid(p); break;
          case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
          case RIL_REQUEST_SIGNAL_STRENGTH: ret =  responseSignalStrength(p); break;
          case RIL_REQUEST_VOICE_REGISTRATION_STATE: ret =  responseStrings(p); break;
          case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  responseStrings(p); break;
          case RIL_REQUEST_OPERATOR: ret =  responseStrings(p); break;
          case RIL_REQUEST_RADIO_POWER: ret =  responseVoid(p); break;
          case RIL_REQUEST_DTMF: ret =  responseVoid(p); break;
          case RIL_REQUEST_SEND_SMS: ret =  responseSMS(p); break;
          case RIL_REQUEST_SEND_SMS_EXPECT_MORE: ret =  responseSMS(p); break;
          case RIL_REQUEST_SETUP_DATA_CALL: ret =  responseSetupDataCall(p); break;
          case RIL_REQUEST_SIM_IO: ret =  responseICC_IO(p); break;
          case RIL_REQUEST_SEND_USSD: ret =  responseVoid(p); break;
          case RIL_REQUEST_CANCEL_USSD: ret =  responseVoid(p); break;
          case RIL_REQUEST_GET_CLIR: ret =  responseInts(p); break;
          case RIL_REQUEST_SET_CLIR: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: ret =  responseCallForward(p); break;
          case RIL_REQUEST_SET_CALL_FORWARD: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_CALL_WAITING: ret =  responseInts(p); break;
          case RIL_REQUEST_SET_CALL_WAITING: ret =  responseVoid(p); break;
          case RIL_REQUEST_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
          case RIL_REQUEST_GET_IMEI: ret =  responseString(p); break;
          case RIL_REQUEST_GET_IMEISV: ret =  responseString(p); break;
          case RIL_REQUEST_ANSWER: ret =  responseVoid(p); break;
          case RIL_REQUEST_DEACTIVATE_DATA_CALL: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_FACILITY_LOCK: ret =  responseInts(p); break;
          case RIL_REQUEST_SET_FACILITY_LOCK: ret =  responseInts(p); break;
          case RIL_REQUEST_CHANGE_BARRING_PASSWORD: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: ret =  responseInts(p); break;
          case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: ret =  responseVoid(p); break;
          case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : ret =  responseOperatorInfos(p); break;
          case RIL_REQUEST_DTMF_START: ret =  responseVoid(p); break;
          case RIL_REQUEST_DTMF_STOP: ret =  responseVoid(p); break;
          case RIL_REQUEST_BASEBAND_VERSION: ret =  responseString(p); break;
          case RIL_REQUEST_SEPARATE_CONNECTION: ret =  responseVoid(p); break;
          case RIL_REQUEST_SET_MUTE: ret =  responseVoid(p); break;
          case RIL_REQUEST_GET_MUTE: ret =  responseInts(p); break;
          case RIL_REQUEST_QUERY_CLIP: ret =  responseInts(p); break;
          case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
          case RIL_REQUEST_DATA_CALL_LIST: ret =  responseDataCallList(p); break;
          case RIL_REQUEST_RESET_RADIO: ret =  responseVoid(p); break;
          case RIL_REQUEST_OEM_HOOK_RAW: ret =  responseRaw(p); break;
          case RIL_REQUEST_OEM_HOOK_STRINGS: ret =  responseStrings(p); break;
          case RIL_REQUEST_SCREEN_STATE: ret =  responseVoid(p); break;
          case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: ret =  responseVoid(p); break;
          case RIL_REQUEST_WRITE_SMS_TO_SIM: ret =  responseInts(p); break;
          case RIL_REQUEST_DELETE_SMS_ON_SIM: ret =  responseVoid(p); break;
          case RIL_REQUEST_SET_BAND_MODE: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: ret =  responseInts(p); break;
          case RIL_REQUEST_STK_GET_PROFILE: ret =  responseString(p); break;
          case RIL_REQUEST_STK_SET_PROFILE: ret =  responseVoid(p); break;
          case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: ret =  responseString(p); break;
          case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: ret =  responseVoid(p); break;
          case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: ret =  responseInts(p); break;
          case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: ret =  responseVoid(p); break;
          case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: ret =  responseVoid(p); break;
          case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: ret =  responseGetPreferredNetworkType(p); break;
          case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: ret = responseCellList(p); break;
          case RIL_REQUEST_SET_LOCATION_UPDATES: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: ret =  responseInts(p); break;
          case RIL_REQUEST_SET_TTY_MODE: ret =  responseVoid(p); break;
          case RIL_REQUEST_QUERY_TTY_MODE: ret =  responseInts(p); break;
          case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseInts(p); break;
          case RIL_REQUEST_CDMA_FLASH: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_BURST_DTMF: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_SEND_SMS: ret =  responseSMS(p); break;
          case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
          case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: ret =  responseGmsBroadcastConfig(p); break;
          case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
          case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: ret =  responseCdmaBroadcastConfig(p); break;
          case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: ret =  responseVoid(p); break;
          case RIL_REQUEST_CDMA_SUBSCRIPTION: ret =  responseStrings(p); break;
          case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: ret =  responseInts(p); break;
          case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: ret =  responseVoid(p); break;
          case RIL_REQUEST_DEVICE_IDENTITY: ret =  responseStrings(p); break;
          case RIL_REQUEST_GET_SMSC_ADDRESS: ret = responseString(p); break;
          case RIL_REQUEST_SET_SMSC_ADDRESS: ret = responseVoid(p); break;
          case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
          case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: ret = responseVoid(p); break;
          case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: ret = responseVoid(p); break;
          case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: ret =  responseInts(p); break;
          case RIL_REQUEST_ISIM_AUTHENTICATION: ret =  responseString(p); break;
          case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU: ret = responseVoid(p); break;
          case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS: ret = responseICC_IO(p); break;
          case RIL_REQUEST_VOICE_RADIO_TECH: ret = responseInts(p); break;
          default:
              throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
          //break;
          }} catch (Throwable tr) {
              // Exceptions here usually mean invalid RIL responses

              Log.w(LOG_TAG, rr.serialString() + "< "
                      + requestToString(rr.mRequest)
                      + " exception, possible invalid RIL response", tr);

              if (rr.mResult != null) {
                  AsyncResult.forMessage(rr.mResult, null, tr);
                  rr.mResult.sendToTarget();
              }
              rr.release();
              return;
          }
      }

      // Here and below fake RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED, see b/7255789.
      // This is needed otherwise we don't automatically transition to the main lock
      // screen when the pin or puk is entered incorrectly.
      switch (rr.mRequest) {
          case RIL_REQUEST_ENTER_SIM_PUK:
          case RIL_REQUEST_ENTER_SIM_PUK2:
              if (mIccStatusChangedRegistrants != null) {
                  if (RILJ_LOGD) {
                      riljLog("ON enter sim puk fakeSimStatusChanged: reg count="
                              + mIccStatusChangedRegistrants.size());
                  }
                  mIccStatusChangedRegistrants.notifyRegistrants();
              }
              break;
      }

      if (error != 0) {
          switch (rr.mRequest) {
              case RIL_REQUEST_ENTER_SIM_PIN:
              case RIL_REQUEST_ENTER_SIM_PIN2:
              case RIL_REQUEST_CHANGE_SIM_PIN:
              case RIL_REQUEST_CHANGE_SIM_PIN2:
              case RIL_REQUEST_SET_FACILITY_LOCK:
                  if (mIccStatusChangedRegistrants != null) {
                      if (RILJ_LOGD) {
                          riljLog("ON some errors fakeSimStatusChanged: reg count="
                                  + mIccStatusChangedRegistrants.size());
                      }
                      mIccStatusChangedRegistrants.notifyRegistrants();
                  }
                  break;
          }

          rr.onError(error, ret);
          rr.release();
          return;
      }

      if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
          + " " + retToString(rr.mRequest, ret));

      if (rr.mResult != null) {
          AsyncResult.forMessage(rr.mResult, ret, null);
          rr.mResult.sendToTarget();
      }

      rr.release();
  }

  
  protected void
  processUnsolicited (Parcel p) {
      int response;
      Object ret;

      response = p.readInt();

      try {switch(response) {
/*
cat libs/telephony/ril_unsol_commands.h \
| egrep "^ *{RIL_" \
| sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: \2(rr, p); break;/'
*/

          case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
          case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED: ret =  responseVoid(p); break;
          case RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED: ret =  responseVoid(p); break;
          case RIL_UNSOL_RESPONSE_NEW_SMS: ret =  responseString(p); break;
          case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT: ret =  responseString(p); break;
          case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM: ret =  responseInts(p); break;
          case RIL_UNSOL_ON_USSD: ret =  responseStrings(p); break;
          case RIL_UNSOL_NITZ_TIME_RECEIVED: ret =  responseString(p); break;
          case RIL_UNSOL_SIGNAL_STRENGTH: ret = responseSignalStrength(p); break;
          case RIL_UNSOL_DATA_CALL_LIST_CHANGED: ret = responseDataCallList(p);break;
          case RIL_UNSOL_SUPP_SVC_NOTIFICATION: ret = responseSuppServiceNotification(p); break;
          case RIL_UNSOL_STK_SESSION_END: ret = responseVoid(p); break;
          case RIL_UNSOL_STK_PROACTIVE_COMMAND: ret = responseString(p); break;
          case RIL_UNSOL_STK_EVENT_NOTIFY: ret = responseString(p); break;
          case RIL_UNSOL_STK_CALL_SETUP: ret = responseInts(p); break;
          case RIL_UNSOL_SIM_SMS_STORAGE_FULL: ret =  responseVoid(p); break;
          case RIL_UNSOL_SIM_REFRESH: ret =  responseSimRefresh(p); break;
          case RIL_UNSOL_CALL_RING: ret =  responseCallRing(p); break;
          case RIL_UNSOL_RESTRICTED_STATE_CHANGED: ret = responseInts(p); break;
          case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED:  ret =  responseVoid(p); break;
          case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS:  ret =  responseCdmaSms(p); break;
          case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:  ret =  responseRaw(p); break;
          case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL:  ret =  responseVoid(p); break;
          case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
          case RIL_UNSOL_CDMA_CALL_WAITING: ret = responseCdmaCallWaiting(p); break;
          case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS: ret = responseInts(p); break;
          case RIL_UNSOL_CDMA_INFO_REC: ret = responseCdmaInformationRecord(p); break;
          case RIL_UNSOL_OEM_HOOK_RAW: ret = responseRaw(p); break;
          case RIL_UNSOL_RINGBACK_TONE: ret = responseInts(p); break;
          case RIL_UNSOL_RESEND_INCALL_MUTE: ret = responseVoid(p); break;
          case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED: ret = responseInts(p); break;
          case RIL_UNSOL_CDMA_PRL_CHANGED: ret = responseInts(p); break;
          case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
          case RIL_UNSOL_RIL_CONNECTED: ret = responseInts(p); break;
          case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED: ret =  responseInts(p); break;
          case RIL_UNSOL_STK_SEND_SMS_RESULT: ret = responseInts(p); break; // Samsung STK

          default:
              throw new RuntimeException("Unrecognized unsol response: " + response);
          //break; (implied)
      }} catch (Throwable tr) {
          Log.e(LOG_TAG, "Exception processing unsol response: " + response +
              "Exception:" + tr.toString());
          return;
      }

      switch(response) {
          case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
              /* has bonus radio state int */
              RadioState newState = getRadioStateFromInt(p.readInt());
              if (RILJ_LOGD) unsljLogMore(response, newState.toString());

              switchToRadioState(newState);
          break;
          case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED:
              if (RILJ_LOGD) unsljLog(response);

              mCallStateRegistrants
                  .notifyRegistrants(new AsyncResult(null, null, null));
          break;
          case RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED:
              if (RILJ_LOGD) unsljLog(response);

              mVoiceNetworkStateRegistrants
                  .notifyRegistrants(new AsyncResult(null, null, null));
          break;
          case RIL_UNSOL_RESPONSE_NEW_SMS: {
              if (RILJ_LOGD) unsljLog(response);

              // FIXME this should move up a layer
              String a[] = new String[2];

              a[1] = (String)ret;

              SmsMessage sms;

              sms = SmsMessage.newFromCMT(a);
              if (mGsmSmsRegistrant != null) {
                  mGsmSmsRegistrant
                      .notifyRegistrant(new AsyncResult(null, sms, null));
              }
          break;
          }
          case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mSmsStatusRegistrant != null) {
                  mSmsStatusRegistrant.notifyRegistrant(
                          new AsyncResult(null, ret, null));
              }
          break;
          case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              int[] smsIndex = (int[])ret;

              if(smsIndex.length == 1) {
                  if (mSmsOnSimRegistrant != null) {
                      mSmsOnSimRegistrant.
                              notifyRegistrant(new AsyncResult(null, smsIndex, null));
                  }
              } else {
                  if (RILJ_LOGD) riljLog(" NEW_SMS_ON_SIM ERROR with wrong length "
                          + smsIndex.length);
              }
          break;
          case RIL_UNSOL_ON_USSD:
              String[] resp = (String[])ret;

              if (resp.length < 2) {
                  resp = new String[2];
                  resp[0] = ((String[])ret)[0];
                  resp[1] = null;
              }
              if (RILJ_LOGD) unsljLogMore(response, resp[0]);
              if (mUSSDRegistrant != null) {
                  mUSSDRegistrant.notifyRegistrant(
                      new AsyncResult (null, resp, null));
              }
          break;
          case RIL_UNSOL_NITZ_TIME_RECEIVED:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              // has bonus long containing milliseconds since boot that the NITZ
              // time was received
              long nitzReceiveTime = p.readLong();

              Object[] result = new Object[2];

              result[0] = ret;
              result[1] = Long.valueOf(nitzReceiveTime);

              boolean ignoreNitz = SystemProperties.getBoolean(
                      TelephonyProperties.PROPERTY_IGNORE_NITZ, false);

              if (ignoreNitz) {
                  if (RILJ_LOGD) riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
              } else {
                  if (mNITZTimeRegistrant != null) {

                      mNITZTimeRegistrant
                          .notifyRegistrant(new AsyncResult (null, result, null));
                  } else {
                      // in case NITZ time registrant isnt registered yet
                      mLastNITZTimeInfo = result;
                  }
              }
          break;

          case RIL_UNSOL_SIGNAL_STRENGTH:
              // Note this is set to "verbose" because it happens
              // frequently
              if (RILJ_LOGV) unsljLogvRet(response, ret);

              if (mSignalStrengthRegistrant != null) {
                  mSignalStrengthRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
          break;
          case RIL_UNSOL_DATA_CALL_LIST_CHANGED:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              boolean oldRil = needsOldRilFeature("skipbrokendatacall");
              if (oldRil && "IP".equals(((ArrayList<DataCallState>)ret).get(0).type))
                  break;

              mDataNetworkStateRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
          break;

          case RIL_UNSOL_SUPP_SVC_NOTIFICATION:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mSsnRegistrant != null) {
                  mSsnRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_STK_SESSION_END:
              if (RILJ_LOGD) unsljLog(response);

              if (mCatSessionEndRegistrant != null) {
                  mCatSessionEndRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_STK_PROACTIVE_COMMAND:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mCatProCmdRegistrant != null) {
                  mCatProCmdRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_STK_EVENT_NOTIFY:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mCatEventRegistrant != null) {
                  mCatEventRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_STK_CALL_SETUP:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mCatCallSetUpRegistrant != null) {
                  mCatCallSetUpRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_SIM_SMS_STORAGE_FULL:
              if (RILJ_LOGD) unsljLog(response);

              if (mIccSmsFullRegistrant != null) {
                  mIccSmsFullRegistrant.notifyRegistrant();
              }
              break;

          case RIL_UNSOL_SIM_REFRESH:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mIccRefreshRegistrants != null) {
                  mIccRefreshRegistrants.notifyRegistrants(
                          new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_CALL_RING:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mRingRegistrant != null) {
                  mRingRegistrant.notifyRegistrant(
                          new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_RESTRICTED_STATE_CHANGED:
              if (RILJ_LOGD) unsljLogvRet(response, ret);
              if (mRestrictedStateRegistrant != null) {
                  mRestrictedStateRegistrant.notifyRegistrant(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED:
              if (RILJ_LOGD) unsljLog(response);

              if (mIccStatusChangedRegistrants != null) {
                  mIccStatusChangedRegistrants.notifyRegistrants();
              }
              break;

          case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS:
              if (RILJ_LOGD) unsljLog(response);

              SmsMessage sms = (SmsMessage) ret;

              if (mCdmaSmsRegistrant != null) {
                  mCdmaSmsRegistrant
                      .notifyRegistrant(new AsyncResult(null, sms, null));
              }
              break;

          case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:
              if (RILJ_LOGD) unsljLog(response);

              if (mGsmBroadcastSmsRegistrant != null) {
                  mGsmBroadcastSmsRegistrant
                      .notifyRegistrant(new AsyncResult(null, ret, null));
              }
              break;

          case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL:
              if (RILJ_LOGD) unsljLog(response);

              if (mIccSmsFullRegistrant != null) {
                  mIccSmsFullRegistrant.notifyRegistrant();
              }
              break;

          case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE:
              if (RILJ_LOGD) unsljLog(response);

              if (mEmergencyCallbackModeRegistrant != null) {
                  mEmergencyCallbackModeRegistrant.notifyRegistrant();
              }
              break;

          case RIL_UNSOL_CDMA_CALL_WAITING:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mCallWaitingInfoRegistrants != null) {
                  mCallWaitingInfoRegistrants.notifyRegistrants(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mOtaProvisionRegistrants != null) {
                  mOtaProvisionRegistrants.notifyRegistrants(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_CDMA_INFO_REC:
              ArrayList<CdmaInformationRecords> listInfoRecs;

              try {
                  listInfoRecs = (ArrayList<CdmaInformationRecords>)ret;
              } catch (ClassCastException e) {
                  Log.e(LOG_TAG, "Unexpected exception casting to listInfoRecs", e);
                  break;
              }

              for (CdmaInformationRecords rec : listInfoRecs) {
                  if (RILJ_LOGD) unsljLogRet(response, rec);
                  notifyRegistrantsCdmaInfoRec(rec);
              }
              break;

          case RIL_UNSOL_OEM_HOOK_RAW:
              if (RILJ_LOGD) unsljLogvRet(response, IccUtils.bytesToHexString((byte[])ret));
              if (mUnsolOemHookRawRegistrant != null) {
                  mUnsolOemHookRawRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
              }
              break;

          case RIL_UNSOL_RINGBACK_TONE:
              if (RILJ_LOGD) unsljLogvRet(response, ret);
              if (mRingbackToneRegistrants != null) {
                  boolean playtone = (((int[])ret)[0] == 1);
                  mRingbackToneRegistrants.notifyRegistrants(
                                      new AsyncResult (null, playtone, null));
              }
              break;

          case RIL_UNSOL_RESEND_INCALL_MUTE:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mResendIncallMuteRegistrants != null) {
                  mResendIncallMuteRegistrants.notifyRegistrants(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mVoiceRadioTechChangedRegistrants != null) {
                  mVoiceRadioTechChangedRegistrants.notifyRegistrants(
                          new AsyncResult(null, ret, null));
              }
              break;

          case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mCdmaSubscriptionChangedRegistrants != null) {
                  mCdmaSubscriptionChangedRegistrants.notifyRegistrants(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_CDMA_PRL_CHANGED:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mCdmaPrlChangedRegistrants != null) {
                  mCdmaPrlChangedRegistrants.notifyRegistrants(
                                      new AsyncResult (null, ret, null));
              }
              break;

          case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE:
              if (RILJ_LOGD) unsljLogRet(response, ret);

              if (mExitEmergencyCallbackModeRegistrants != null) {
                  mExitEmergencyCallbackModeRegistrants.notifyRegistrants(
                                      new AsyncResult (null, null, null));
              }
              break;

          case RIL_UNSOL_RIL_CONNECTED: {
              if (RILJ_LOGD) unsljLogRet(response, ret);

              // Initial conditions
              setRadioPower(false, null);
              setPreferredNetworkType(mPreferredNetworkType, null);
              setCdmaSubscriptionSource(mCdmaSubscription, null);
              notifyRegistrantsRilConnectionChanged(((int[])ret)[0]);
              break;
          }

          // Samsung STK
          case RIL_UNSOL_STK_SEND_SMS_RESULT:
              if (Resources.getSystem().
                      getBoolean(com.android.internal.R.bool.config_samsung_stk)) {
                  if (RILJ_LOGD) unsljLogRet(response, ret);

                  if (mCatSendSmsResultRegistrant != null) {
                      mCatSendSmsResultRegistrant.notifyRegistrant(
                              new AsyncResult (null, ret, null));
                  }
              }
              break;
      }
  }

  
  
protected Object responseSignalStrength(Parcel paramParcel)
  {
 
    int mGsmSignalStrength=paramParcel.readInt(); // Valid values are (0-31, 99) as defined in TS 27.007 8.5
    int mGsmBitErrorRate=paramParcel.readInt();   // bit error rate (0-7, 99) as defined in TS 27.007 8.5
    int mCdmaDbm=paramParcel.readInt();   // This value is the RSSI value
    int mCdmaEcio=paramParcel.readInt();  // This value is the Ec/Io
    int mEvdoDbm=paramParcel.readInt();   // This value is the EVDO RSSI value
    int mEvdoEcio=paramParcel.readInt();  // This value is the EVDO Ec/Io
    int mEvdoSnr=paramParcel.readInt();   // Valid values are 0-8.  8 is the highest signal to noise ratio
    int mLteSignalStrength=paramParcel.readInt();
    int mLteRsrp=paramParcel.readInt();
    int mLteRsrq=paramParcel.readInt();
    int mLteRssnr=paramParcel.readInt();
    int mLteCqi=paramParcel.readInt();  
    int isGsmInt=paramParcel.readInt(); // This value is set by the ServiceStateTracker onSignalStrengthResult

    boolean isGsm=true;
    
    if (isGsmInt == 0)
    {  	
        isGsm = false;
        mGsmSignalStrength=(mCdmaDbm+113)/2;
    }
    
    return new SignalStrength(mGsmSignalStrength, mGsmBitErrorRate, mCdmaDbm, mCdmaEcio, mEvdoDbm, mEvdoEcio, mEvdoSnr, isGsm);
  }
}

