package com.mdd.prepaid_lib_flutter_null_safety

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.NonNull
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import android.util.Log
import androidx.annotation.RequiresApi
import com.mdd.android_mdd_lib.CardInfoObject
import com.mdd.android_mdd_lib.EncryptGCM
import com.mdd.android_mdd_lib.MainCardProcessor
import com.mdd.android_mdd_lib.SerializeObject
import com.mdd.android_mdd_lib.api.request.ConfirmBalanceRequest
import com.mdd.android_mdd_lib.api.request.ReversalRequest
import com.mdd.android_mdd_lib.api.request.UnlockV1Request
import com.mdd.android_mdd_lib.api.request.UpdateBalanceRequest
import com.mdd.android_mdd_lib.api.response.ConfirmBalanceResponse
import com.mdd.android_mdd_lib.api.response.ReversalResponse
import com.mdd.android_mdd_lib.api.response.UnlockV1Response
import com.mdd.android_mdd_lib.api.response.UpdateBalanceResponse

/** UnikLibFlutterPlugin */
@RequiresApi(Build.VERSION_CODES.O)
class UnikLibFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var reversalObject: ReversalRequest
    private var approvalCode: Long = 0
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var mainCardProcessor: MainCardProcessor
    private val fileName = "android_mdd_lib.txt"
    private var isMandiriEnable = false
    private var isBniEnable = false
    private var isBriEnable = false
    private var isBcaEnable = false
    private var isDkiEnable = false
    private var mid = ""
    private var token = ""
    private var tid = ""
    private var environment = 0


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "unik_lib_flutter")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "initUnikLib" -> {
                val reqMid = call.argument<String>("mid")
                val env = call.argument<Int>("env")
                var preferenceManagers = PreferenceManagers()
                var initResp = preferenceManagers.getData(fileName, context)

                var tmDevice = ""
                val androidId: String =
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                tmDevice = try {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    "" + tm.deviceId
                } catch (e: Exception) {
                    androidId
                }

                val manufacturer = Build.MANUFACTURER
                val modelDevice = Build.MODEL

//                Log.d("ANDROID_ID", androidId)
//                Log.d("tmDevice", tmDevice)
//                Log.d("modelDevice", modelDevice)
//                Log.d("manufacturer", manufacturer)
                val brand = Build.BRAND
                val sdk = Build.VERSION.SDK_INT
                val release = Build.VERSION.RELEASE
                val osInstalledDate = Date(Build.TIME)
                val calendar = Calendar.getInstance()
                calendar.time = osInstalledDate
                Log.i("MW", "time from Build.TIME = " + calendar.get(Calendar.YEAR).toString())
                var sn: String = if (tmDevice == "")
                    androidId
                else
                    tmDevice
                val year = calendar.get(Calendar.YEAR).toString()
                val unlockV1Request = UnlockV1Request(
                    sn,
                    modelDevice,
                    manufacturer,
                    sdk.toString(),
                    release,
                    brand,
                    year
                )
                if (env != null && reqMid != null) {
                    environment = env

                    result.success(readFile(reqMid, unlockV1Request))
                }
            }
            "apduIsMyCard" -> {
                result.success(mainCardProcessor.getApduIsMyCard())
            }
            "apduCardInfo" -> {
                result.success(mainCardProcessor.getApduMandiriCardInfo())
            }
            "apduBalance" -> {
                result.success(mainCardProcessor.getApduBalance())
            }
            "apduCardAttr" -> {
                result.success(mainCardProcessor.getApduCardAttr())
            }
            "apduCardCert" -> {
                result.success(mainCardProcessor.getApduMandiriCert())
            }
            "apduCrn" -> {
                result.success(mainCardProcessor.getCrnBni())
            }
            "apduManipulate" -> {
                result.success(mainCardProcessor.getApduManipulateBni())
            }
            "getHistory" -> {
                val apdu = call.argument<String>("apduCardInfo")
                val bankType = call.argument<String>("bankType")
                val cardInfo = CardInfoObject(apdu!!, bankType!!, "", "", 0)
                result.success(mainCardProcessor.getApduHistory(cardInfo))
            }
            "parsingHistory" -> {
                val rApdu = call.argument<String>("rApdu")
                val bankType = call.argument<String>("bankType")
                val cardInfo = CardInfoObject(rApdu!!, bankType!!, "", "", 0)
//                Log.d("UnikLibFlutterPlugin", "object history ${Gson().toJson(cardInfo)}")
                result.success(mainCardProcessor.parsingCardHistory(cardInfo))
            }
            "parseBalance" -> {
                val apduBalance = call.argument<String>("apduBalance")
                val bankType = call.argument<String>("bankType")
                var cardInfo = CardInfoObject(apduBalance!!, bankType!!, "", "", 0)
                result.success(mainCardProcessor.parsingBalance(cardInfo))
            }
            "updateBalance" -> {
                val uid = call.argument<String>("cardUid")
                val bankType = call.argument<String>("bankType")
                val cardNumber = call.argument<String>("cardNumber")
                val balance = call.argument<String>("balance")
                val cardAttr = call.argument<String>("cardAttr")
                val respCardInfo = call.argument<String>("respCardInfo")
                val email = call.argument<String>("email")
                val phoneNumber = call.argument<String>("phoneNumber")
                try {
                    var apprCode = PreferenceManagers()
                        .getData("APPCODE $cardNumber", context)
                    approvalCode = apprCode?.toLong() ?: System.currentTimeMillis() / 1000

                    reversalObject = ReversalRequest(
                        tid, token,
                        mid, "0",
                        cardNumber!!, cardAttr!!,
                        balance!!, approvalCode.toString(),
                        respCardInfo!!, uid!!,
                        "", phoneNumber!!, "", email!!,
                        ""
                    )


                    val cardInfo = CardInfoObject("", bankType!!, cardNumber, uid, 0)
                    val request = UpdateBalanceRequest(
                        tid, token,
                        mid, "",
                        cardNumber, cardAttr,
                        balance,
                        balance, approvalCode.toString(),
                        respCardInfo, uid, "",
                        "", phoneNumber, email
                    )

                    Log.d("UnikLibFlutterPlugin", "request ubal : ${Gson().toJson(request)}")

                    thread(true) {
                        val updateBalance: String? = mainCardProcessor.updateBalance(
                            cardInfo,
                            request,
                            environment
                        )
//
                        if (!updateBalance.equals("null", ignoreCase = true)) {
                            if (updateBalance != "") {
                                var responseUpdate: UpdateBalanceResponse = Gson().fromJson(
                                    updateBalance, UpdateBalanceResponse::class.java
                                )
                                if (responseUpdate.response.code == "200") {
                                    PreferenceManagers().setDataWithSameKey(
                                        "APPCODE$cardNumber",
                                        approvalCode.toString(),
                                        context
                                    )
                                    reversalObject.sam_data = responseUpdate.data.dataToCard
                                    if (bankType == "MANDIRI") {
                                        // save data reversal if bank mandiri
                                        if (responseUpdate.data.appletType == "OLD") {
                                            PreferenceManagers().setDataWithSameKey(
                                                cardNumber,
                                                Gson().toJson(reversalObject),
                                                context
                                            )
//                                        Log.d(
//                                            "UnikLibFlutter",
//                                            "--> save reversal data with key $cardNumber =  ${
//                                                Gson().toJson(reversalObject)
//                                            }"
//                                        )
                                        }
                                        activity.runOnUiThread {
                                            result.success(
                                                String.format(
                                                    "%s,%s,%s",
                                                    responseUpdate.response.code,
                                                    responseUpdate.data.dataToCard,
                                                    responseUpdate.data.appletType
                                                )
                                            )
                                        }

                                    } else {
                                        activity.runOnUiThread {
                                            result.success(
                                                String.format(
                                                    "%s,%s,%s",
                                                    responseUpdate.response.code,
                                                    responseUpdate.data.dataToCard.substring(
                                                        responseUpdate.data.dataToCard.length - 32
                                                    ),
                                                    responseUpdate.data.reff_no
                                                )
                                            )
                                        }
                                    }

                                } else {
                                    activity.runOnUiThread {
                                        result.success(
                                            String.format(
                                                "%s,%s",
                                                responseUpdate.response.code,
                                                responseUpdate.response.message
                                            )
                                        )
                                    }
                                }
                            } else {
                                activity.runOnUiThread {
                                    result.error(
                                        "0001",
                                        "Failed update balance",
                                        "Something went wrong"
                                    )
                                }
                            }
                        } else {
                            activity.runOnUiThread {
                                result.error(
                                    "0001",
                                    "Failed update balance",
                                    "Something went wrong"
                                )
                            }
                        }
                    }
                } catch (exception: Exception) {
                    result.error("0001", exception.localizedMessage, "Something went wrong")
                }
            }
            "confirmUpdate" -> {
                val uid = call.argument<String>("cardUid")
                val bankType = call.argument<String>("bankType")
                val cardNumber = call.argument<String>("cardNumber")
                val samData = call.argument<String>("samData")
                val reff_no = call.argument<String>("reff_no")
                val balance = call.argument<String>("balance")
                var writeStatus = call.argument<String>("writeStatus")
                val email = call.argument<String>("email")
                val phoneNumber = call.argument<String>("phoneNumber")
                val cardInfo = CardInfoObject("", bankType!!, cardNumber!!, uid!!, 0)
                var request = ConfirmBalanceRequest(
                    tid,
                    token,
                    mid,
                    cardNumber,
                    samData!!,
                    phoneNumber!!,
                    email!!,
                    approvalCode.toString(),
                    reff_no!!,
                    balance!!,
                    writeStatus!!
                )

                thread(true) {
                    val respConfirmBalance: String? = mainCardProcessor.confirmBalance(
                        cardInfo,
                        request,
                        environment
                    )

                    if (!respConfirmBalance.equals("null", ignoreCase = true)) {

                        if (respConfirmBalance != "") {
                            var responseConfirm: ConfirmBalanceResponse = Gson().fromJson(
                                respConfirmBalance, ConfirmBalanceResponse::class.java
                            )
                            if (writeStatus == "COMPLETED") {
                                // clear data reversal
                                PreferenceManagers()
                                    .clearDataWithKey(cardNumber, context)
                                PreferenceManagers()
                                    .clearDataWithKey("APPCODE$cardNumber", context)
                                activity.runOnUiThread {
                                    result.success(responseConfirm.response.message)
                                }
                            } else {
                                if (responseConfirm.response.code == "200") {
                                    if (responseConfirm.data.dataToCard == "") {

                                        // clear data reversal
                                        PreferenceManagers()
                                            .clearDataWithKey(cardNumber, context)
                                        PreferenceManagers().clearDataWithKey(
                                            "APPCODE$cardNumber",
                                            context
                                        )
                                    } else {

                                        if (bankType == "MANDIRI") {
                                            // get data reversal and update it
                                            var dataRev =
                                                PreferenceManagers()
                                                    .getData(cardNumber, context)

                                            if (dataRev == null) {
                                                PreferenceManagers().setDataWithSameKey(
                                                    cardNumber,
                                                    Gson().toJson(reversalObject),
                                                    context
                                                )
                                                dataRev = PreferenceManagers()
                                                    .getData(
                                                    cardNumber,
                                                    context
                                                )
                                            }

                                            var reversalObject: ReversalRequest =
                                                Gson().fromJson(
                                                    dataRev,
                                                    ReversalRequest::class.java
                                                )
                                            reversalObject.sam_data =
                                                responseConfirm.data.dataToCard
                                            reversalObject.applet = responseConfirm.data.appletType

                                            PreferenceManagers().setDataWithSameKey(
                                                reversalObject.card_no,
                                                Gson().toJson(reversalObject),
                                                context
                                            )

//                                    Log.d(
//                                        "UnikLibFlutter",
//                                        "--> update data reversal after confirm ${
//                                            Gson().toJson(reversalObject)
//                                        }"
//                                    )
                                        }
                                    }
                                    if (bankType == "MANDIRI") {
                                        var report = String.format(
                                            "%s,%s,%s",
                                            responseConfirm.data.dataToCard,
                                            responseConfirm.data.updateStatus,
                                            responseConfirm.data.appletType
                                        )
                                        activity.runOnUiThread { result.success(report) }
                                    } else {
                                        var report =
                                            String.format(
                                                "%s,%s",
                                                "N/A",
                                                responseConfirm.data.status
                                            )
                                        activity.runOnUiThread { result.success(report) }
                                    }
                                } else {
                                    activity.runOnUiThread {
                                        result.success(responseConfirm.response.message)
                                    }
                                }
                            }
                        } else {
                            activity.runOnUiThread { result.success("Something went wrong") }
                        }

                    } else {
                        activity.runOnUiThread { result.success("Something went wrong") }
                    }
                }
            }
            "checkingReversal" -> {
                val cardNumber = call.argument<String>("cardNumber")
                var samData = call.argument<String>("samData")
                var mode = call.argument<String>("mode")
                var dataRev = PreferenceManagers()
                    .getData(cardNumber, context)
                /*Log.d(
                    "UnikLibFlutter",
                    "--> data reversal with key cardNumber $cardNumber = $dataRev"
                )*/
                if (dataRev != null) {
                    thread(true) {
                        var reversalObject: ReversalRequest =
                            Gson().fromJson(dataRev, ReversalRequest::class.java)
                        if (reversalObject.card_no == cardNumber) {
//                            Log.d("UnikLibFlutter", "--> reversal found $dataRev")

                            if (reversalObject.applet == "NEW" && mode!! == "REVERSAL_LOOP") {
                                val samCommand =
                                    mainCardProcessor.getApduMandiriGetDataForSam()
                                val apduCert = mainCardProcessor.getApduMandiriCert()
                                reversalObject.mode = "REVERSAL_NEW"
                                val reversalNew =
                                    String.format("%s,%s,%s", "NEW", samCommand, apduCert)
                                activity.runOnUiThread {
                                    /*Log.d(
                                        "UnikLibFlutter",
                                        "--> reversal message $reversalNew"
                                    )*/
                                    result.success(reversalNew)
                                }

                            } else {
                                if (samData != "") {
                                    reversalObject.sam_data = samData!!
                                    reversalObject.mode = mode!!
                                }

//                                Log.d("UnikLibFlutter", "--> Sam Data $samData")
                                val cardInfo = CardInfoObject(
                                    "",
                                    "MANDIRI",
                                    cardNumber,
                                    reversalObject.card_uid,
                                    reversalObject.last_balance.toInt()
                                )
                                Log.d("UnikLibFlutter", "env --> $environment")
                                var reversalObject: String? = mainCardProcessor.reversalMandiri(
                                    cardInfo,
                                    reversalObject,
                                    environment
                                )
                                if (!reversalObject.equals("null", ignoreCase = true)) {
                                    if (reversalObject != "") {
                                        var revObj: ReversalResponse =
                                            Gson().fromJson(
                                                reversalObject,
                                                ReversalResponse::class.java
                                            )

                                        if (revObj.data.reversalMessage == "REVERSAL_DONE" || revObj.data.reversalMessage == "" || revObj.response.message.contains(
                                                "Not Allowed Reversal"
                                            )
                                        ) {
                                            // clear data reversal
                                            PreferenceManagers().clearDataWithKey(
                                                cardNumber,
                                                context
                                            )
                                            PreferenceManagers().clearDataWithKey(
                                                "APPCODE$cardNumber",
                                                context
                                            )
                                        }

                                        activity.runOnUiThread {
                                            /*Log.d(
                                                "UnikLibFlutter",
                                                "--> reversal message ${revObj.data.reversalMessage}"
                                            )*/
                                            result.success(revObj.data.reversalMessage)
                                        }
                                    } else {
                                        activity.runOnUiThread { result.success("Something Went Wrong") }
                                    }

                                } else {
                                    activity.runOnUiThread { result.success("Something Went Wrong") }
                                }

                            }

                        } else {
                            // todo for reversal not found
                            Log.d("UnikLibFlutter", "--> Reversal not found")
                            activity.runOnUiThread {
                                result.success("")
                            }
                        }
                    }

                } else {
                    Log.d("UnikLibFlutter", "--> Reversal not found")
                    result.success("")
                }
            }
            "composeBNI" -> {
                val apduPurse = call.argument<String>("apduPurse")
                val cardInfo = CardInfoObject(apduPurse!!, "BNI", "", "", 0)
                var res = mainCardProcessor.getComposeBniCardInfo(cardInfo)
                result.success(res)
            }
            "createCrypto" -> {
                val crypto = call.argument<String>("crypto")
                val rrn = call.argument<String>("rrn")
                var res = mainCardProcessor.createCryptoBni(crypto!!, rrn!!)
                result.success(res)

            }
            "encryptGCM" -> {
                val plainText = call.argument<String>("message")
                var encrypt = EncryptGCM()

                result.success(encrypt.encryptGcm(plainText!!))
            }
            "decryptGCM" -> {
                val encryptedText = call.argument<String>("message")
                var encrypt = EncryptGCM()

//                println(encryptedText!!)
                var plainText = encrypt.decryptGcm(encryptedText!!)
//                println(plainText)
                var serializeObject = SerializeObject()
                var jsonString = serializeObject.doSerialize(plainText)
                result.success(jsonString)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun readFile(reqMid: String, unlockV1Request: UnlockV1Request): Boolean {
        var isSuccess: Boolean
        try {
            val encrypt = EncryptGCM()
//            val fileUtils = FileUtils(context)
//            fileUtils.createFile()
//            val respFile = fileUtils.readFileAsLinesUsingUseLines(fileName)

            val preferenceManagers = PreferenceManagers()
            val respFile = preferenceManagers.getData(fileName, context)
            mainCardProcessor = MainCardProcessor(context, "")

            print("init mainCardProcessor1")
            if (respFile == null) {
                print("respFile null")

                val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
                StrictMode.setThreadPolicy(policy)
                print("hit api unlock")

                val respApi = mainCardProcessor.unlockV1(reqMid, unlockV1Request, environment)

//                print("respApi $respApi")
                val unlockV1Response = Gson().fromJson(respApi, UnlockV1Response::class.java)

                isMandiriEnable = unlockV1Response.mandiri == 1
                isBniEnable = unlockV1Response.bni == 1
                isBriEnable = unlockV1Response.bri == 1
                isBcaEnable = unlockV1Response.bca == 1
                isDkiEnable = unlockV1Response.dki == 1
                mid = unlockV1Response.mid
                tid = unlockV1Response.device_id
                token = unlockV1Response.token

                Log.d("isMandiriEnable", isMandiriEnable.toString())
                Log.d("isBniEnable", isBniEnable.toString())
                Log.d("isBriEnable", isBriEnable.toString())
                Log.d("isBcaEnable", isBcaEnable.toString())
                Log.d("isDkiEnable", isDkiEnable.toString())

//                fileUtils.createFile()

                preferenceManagers.setData(fileName, encrypt.encryptGcm(respApi), context)
//                println("MainCardProcessor $respApi")
                mainCardProcessor = MainCardProcessor(context, respApi)

//                fileUtils.writeFile(encrypt.encryptText(respApi))
            } else {
                println("respFile not null")

                val decResp = encrypt.decryptGcm(respFile)
                var unlockV1Response = Gson().fromJson(decResp, UnlockV1Response::class.java)
                val date = Date()
                val strDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                if (strDate.equals(unlockV1Response.expired_at)) {
                    val respApi = mainCardProcessor.unlockV1(
                        mid,
                        unlockV1Request,
                        0
                    ) /*force set host init to dev*/
                    unlockV1Response = Gson().fromJson(
                        encrypt.decryptGcm(respApi),
                        UnlockV1Response::class.java
                    )
                    if (unlockV1Response.code == "200") {
                        isMandiriEnable = unlockV1Response.mandiri == 1
                        isBniEnable = unlockV1Response.bni == 1
                        isBriEnable = unlockV1Response.bri == 1
                        isBcaEnable = unlockV1Response.bca == 1
                        isDkiEnable = unlockV1Response.dki == 1
                        mid = unlockV1Response.mid
                        tid = unlockV1Response.device_id
                        token = unlockV1Response.token

                        Log.d("isMandiriEnable", isMandiriEnable.toString())
                        Log.d("isBniEnable", isBniEnable.toString())
                        Log.d("isBriEnable", isBriEnable.toString())
                        Log.d("isBcaEnable", isBcaEnable.toString())
                        Log.d("isDkiEnable", isDkiEnable.toString())
//                        fileUtils.createFile()
//                        fileUtils.writeFile(respApi)
                        mainCardProcessor = MainCardProcessor(context, respApi)

                        preferenceManagers.setData(fileName, encrypt.encryptGcm(respApi), context)
                    }
                } else {
                    print("no expired")

                    isMandiriEnable = unlockV1Response.mandiri == 1
                    isBniEnable = unlockV1Response.bni == 1
                    isBriEnable = unlockV1Response.bri == 1
                    isBcaEnable = unlockV1Response.bca == 1
                    isDkiEnable = unlockV1Response.dki == 1
                    mid = unlockV1Response.mid
                    tid = unlockV1Response.device_id
                    token = unlockV1Response.token

                    Log.d("tid", tid)
                    Log.d("isMandiriEnable", isMandiriEnable.toString())
                    Log.d("isBniEnable", isBniEnable.toString())
                    Log.d("isBriEnable", isBriEnable.toString())
                    Log.d("isBcaEnable", isBcaEnable.toString())
                    Log.d("isDkiEnable", isDkiEnable.toString())
                }
            }
            isSuccess = true
        } catch (e: Exception) {
            e.printStackTrace()
            isSuccess = false
        }
        return isSuccess
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }

}
