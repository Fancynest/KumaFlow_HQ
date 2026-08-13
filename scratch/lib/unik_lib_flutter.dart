import 'dart:async';
import 'dart:convert';

import 'package:convert/convert.dart';
import 'package:flutter/services.dart';
import 'package:flutter_nfc_kit/flutter_nfc_kit.dart';

import 'ApduResponse.dart';

class UnikLibFlutter {
  static const MethodChannel _channel = const MethodChannel('unik_lib_flutter');
  static const String WAITING_STATUS = "WAITING";
  static const String DONE_STATUS = "DONE";

  /// get platform version
  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// init unik library
  static Future<bool> initUnikLib(String mid, int env) async {
    bool isSuccess = false;
    try {
      isSuccess = await _channel.invokeMethod(
          'initUnikLib', <String, dynamic>{'mid': mid, 'env': env});
    } catch (error) {
      isSuccess = false;
    }
    return isSuccess;
  }

  /// get card info
  static Future<bool> getCardInfo(List<String> cardUid, List<String> cardNumber,
      List<String> balance, List<String> bankName,
      {List<String?>? cardAttr,
      List<String?>? cardInfo,
      List<String?>? rrn,
      bool? startPooling,
      List<String?>? beforeBalance,
      Function(bool)? callbackTimeout,
      Function(bool)? errorNfc}) async {
    print("card info $cardInfo");
    print("card info is null = ${cardInfo == null}");
    if (beforeBalance == null) beforeBalance = [''];
    if (cardAttr == null) cardAttr = [''];
    if (cardInfo == null) cardInfo = [''];

    bool res = false;
    String apduClSelectApp = await _channel.invokeMethod('apduIsMyCard');
    String apduClCardInfo = await _channel.invokeMethod('apduCardInfo');
    String apduClBalance = await _channel.invokeMethod('apduBalance');
    String apduClCardAttr = await _channel.invokeMethod('apduCardAttr');
    String? selectApp;

    print('apduClSelectApp $apduClSelectApp');
    print('apduClCardInfo $apduClCardInfo');
    print('apduClBalance $apduClBalance');
    print('apduClCardAttr $apduClCardAttr');

    try {
      if (startPooling!) {
        NFCTag tag = await FlutterNfcKit.poll(
            iosAlertMessage: "Tempelkan kartumu pada iPhone");
        cardUid[0] = tag.id;
      }

      var dataSelect = jsonDecode(apduClSelectApp);
      var dataBalance = jsonDecode(apduClBalance);
      var dataCardAttr = jsonDecode(apduClCardAttr);

      List<ApduResponse> listData = dataSelect
          .map<ApduResponse>((result) => new ApduResponse.fromJson(result))
          .toList();
      List<ApduResponse> listApduBalance = dataBalance
          .map<ApduResponse>((result) => new ApduResponse.fromJson(result))
          .toList();
      List<ApduResponse> listApduCardAttr = dataCardAttr
          .map<ApduResponse>((result) => new ApduResponse.fromJson(result))
          .toList();

      /// apdu cl select app
      for (ApduResponse s in listData) {
        selectApp = await FlutterNfcKit.transceive(s.apdu);
        if (_apduIsOke(selectApp!)) {
          bankName[0] = s.bankType;
          print("bank name ${bankName[0]}");
          break;
        }
      }

      for (ApduResponse b in listApduBalance) {
        if (b.bankType == bankName[0]) {
          String rApduCardAttr = "";

          if (bankName[0] == "MANDIRI") {
            /// when bank name mandiri
            String apduBalance = await FlutterNfcKit.transceive(b.apdu);

            cardInfo[0] = await FlutterNfcKit.transceive(apduClCardInfo);
            for (ApduResponse apduCardAttr in listApduCardAttr) {
              if (apduCardAttr.bankType == bankName[0])
                rApduCardAttr =
                    await FlutterNfcKit.transceive(apduCardAttr.apdu);
            }

            String result = await _channel.invokeMethod(
                'parseBalance', <String, dynamic>{
              'apduBalance': "$apduBalance,${cardInfo[0]}",
              'bankType': bankName[0]
            });
            print("result $result");
            List<String> items = result.split(",");
            print("result = $result");
            balance[0] = items[0];
            beforeBalance[0] = items[0];
            cardNumber[0] = items[1];
            cardAttr[0] = rApduCardAttr;
          } else if (bankName[0] == "BNI") {
            /// when bank name bni
            for (ApduResponse apduCardAttr in listApduCardAttr) {
              if (apduCardAttr.bankType == bankName[0]) {
                print("apduCardAttr.apdu : ${apduCardAttr.apdu}");
                rApduCardAttr =
                    await FlutterNfcKit.transceive(apduCardAttr.apdu);
                print("rApduCardAttr : $rApduCardAttr");
              }
            }

            String apduBalance = await FlutterNfcKit.transceive(b.apdu);

            String purse = apduBalance.substring(0, apduBalance.length - 4);
            String crn = rApduCardAttr.substring(0, rApduCardAttr.length - 4);
            String rRrn = b.apdu.substring(14, 30);

            String subRrn = rRrn;
            print("purse : $purse");
            print("crn : $crn");
            print("subRrn : $subRrn");

            if (rrn != null) rrn[0] = rRrn;

            String rawInfo = purse + crn + rRrn;
            print("rawInfo : $rawInfo");

            String composeBni = await _channel.invokeMethod(
                'composeBNI', <String, dynamic>{'apduPurse': rawInfo});

            String result = await _channel.invokeMethod(
                'parseBalance', <String, dynamic>{
              'apduBalance': purse,
              'bankType': bankName[0]
            });
            print("result $result");
            List<String> items = result.split(",");
            balance[0] = items[0];
            beforeBalance[0] = items[0];
            cardNumber[0] = items[1];
            cardInfo[0] = composeBni;
          } else if (bankName[0] == "BCA") {
            /// when bank name bca
            List<String> listApduBca = b.apdu.split(",");
            // String apduFirst = listApduBca[0];
            String apduSecond = listApduBca[1];
            String apduLast = listApduBca[2];
            // String rApduFirst = await FlutterNfcKit.transceive(apduFirst);
            String rApduSecond = await FlutterNfcKit.transceive(apduSecond);
            String rApduLast = await FlutterNfcKit.transceive(apduLast +
                rApduSecond.substring(0, rApduSecond.length - 4) +
                "29");
            String result = await _channel.invokeMethod(
                'parseBalance', <String, dynamic>{
              'apduBalance': rApduLast,
              'bankType': bankName[0]
            });
            print("result $result");
            List<String> items = result.split(",");
            balance[0] = items[0];
            beforeBalance[0] = items[0];
            cardNumber[0] = items[1];
          } else if (bankName[0] == "DKI-NEW") {
            /// when bank name dki-new
            String rApduBalance = await FlutterNfcKit.transceive(b.apdu);
            String result = await _channel.invokeMethod(
                'parseBalance', <String, dynamic>{
              'apduBalance': "$selectApp,$rApduBalance",
              'bankType': "DKI"
            });
            print("result $result");
            List<String> items = result.split(",");
            balance[0] = items[0];
            beforeBalance[0] = items[0];
            cardNumber[0] = items[1];
          }

          print("cardNumber = ${cardNumber[0]}, balance ${balance[0]}");
          res = true;
        }
      }
    } catch (error, stack) {
      print("stack $stack");
      res = false;
      if (error.toString().toLowerCase().contains("polling tag timeout"))
        callbackTimeout!(true);
      if (error.toString().toLowerCase().contains("nfc not available"))
        errorNfc!(true);
      print("error operation ${error.toString()}");
    }
    return res;
  }

  static Future<bool> updateBalance(
      List<String> status,
      List<String> cardNumber,
      List<String> balance,
      List<String> bankName,
      List<String> beforeBalance,
      String email,
      String phoneNumber,
      {Function(String)? callbackState,
      Function(bool)? callbackTimeout,
      Function(bool)? errorNfc}) async {
    bool result = false;
    List<String> cardUid = [''];
    List<String> cardAttr = [''];
    List<String> cardInfo = [''];
    List<String> lRrn = [''];
    bool isTimeout = false;
    bool isErrorNfc = false;

    try {
      bool isSuccess = await getCardInfo(cardUid, cardNumber, balance, bankName,
          cardAttr: cardAttr,
          cardInfo: cardInfo,
          rrn: lRrn,
          startPooling: true,
          beforeBalance: beforeBalance,
          callbackTimeout: (bool value) => isTimeout = value,
          errorNfc: (bool value) => isErrorNfc = value);

      if (isSuccess) {
        /// tell your app for waiting operation
        callbackState!(WAITING_STATUS);

        /// checking reversal, do reversal if exist
        if (await _doReversalIfExist(
            cardNumber[0], "", "REVERSAL_LOOP", status)) {
          /// get card info again before confirm
          await getCardInfo(cardUid, cardNumber, balance, bankName,
              cardAttr: cardAttr,
              cardInfo: cardInfo,
              rrn: lRrn,
              startPooling: false,
              beforeBalance: beforeBalance,
              callbackTimeout: (bool value) => isTimeout = value,
              errorNfc: (bool value) => isErrorNfc = value);

          /// update balance
          String resUpdate =
              await _channel.invokeMethod('updateBalance', <String, dynamic>{
            'cardNumber': cardNumber[0],
            'cardUid': cardUid[0],
            'bankType': bankName[0],
            'balance': balance[0],
            'cardAttr': cardAttr[0],
            'respCardInfo': cardInfo[0],
            'email': email,
            'phoneNumber': phoneNumber
          }).onError((error, stackTrace) {
            status[0] = "Connection error";
          });

          List<String> items = resUpdate.toString().split(",");
          print("resUpdate.toString() : ${resUpdate.toString()}");
          print("rrn : ${lRrn[0]}");

          if (items[0] == "200") {
            if (bankName[0] == "MANDIRI") {
              String apduClCert = await _channel.invokeMethod('apduCardCert');
              String rApdu = await FlutterNfcKit.transceive(items[1]);
              if (items[2] == "NEW") {
                String rApduCert = await FlutterNfcKit.transceive(apduClCert);
                rApdu = rApdu.substring(0, rApdu.length - 4) +
                    rApduCert.substring(0, rApduCert.length - 4);
              }

              /// confirm update mandiri
              result = await _confirmUpdate(
                  cardUid[0],
                  bankName[0],
                  cardNumber[0],
                  rApdu,
                  status,
                  "",
                  balance[0],
                  "",
                  email,
                  phoneNumber);

              /// get balance again after confirm for mandiri
              await getCardInfo(cardUid, cardNumber, balance, bankName,
                  cardAttr: cardAttr, cardInfo: cardInfo, startPooling: false);
            } else {
              /// confirm update bni
              String crypto = await _channel.invokeMethod('createCrypto',
                  <String, dynamic>{'crypto': items[1], 'rrn': lRrn[0]});

              print("crypto : $crypto");

              String? rApdu;
              for (int i = 3; i != 0; i--) {
                rApdu = await FlutterNfcKit.transceive(crypto);
                if (_apduIsOke(rApdu!)) {
                  break;
                }
              }

              /// get balance again after confirm for bni
              await getCardInfo(cardUid, cardNumber, balance, bankName,
                  cardAttr: cardAttr, cardInfo: cardInfo, startPooling: false);

              if (_apduIsOke(rApdu!)) {
                result = await _confirmUpdate(
                    cardUid[0],
                    bankName[0],
                    cardNumber[0],
                    rApdu,
                    status,
                    items[2],
                    balance[0],
                    "",
                    email,
                    phoneNumber);
              } else {
                status[0] = items[1];
                result = false;
              }
            }
          } else {
            if (items[1] == "Pending Topup History Not Found") {
              status[0] = "No Pending Balance";
            } else {
              status[0] = items[1];
            }
            result = false;
          }
        }
      } else {
        result = false;
        callbackTimeout!(isTimeout);
        errorNfc!(isErrorNfc);
        status[0] = (isTimeout)
            ? "Timeout polling card"
            : (isErrorNfc)
                ? "NFC Not Available"
                : "Something went wrong";
      }
    } catch (error) {
      result = false;
      callbackTimeout!(isTimeout);
      errorNfc!(isErrorNfc);
      status[0] = (isTimeout)
          ? "Timeout polling card"
          : (isErrorNfc)
              ? "NFC Not Available"
              : "Something went wrong";
    }

    callbackState!(DONE_STATUS);

    /// when done operation
    return result;
  }

  static Future<bool> _doReversalIfExist(String cardNumber, String samData,
      String mode, List<String> status) async {
    String revResult = await _channel.invokeMethod(
        "checkingReversal", <String, dynamic>{
      'cardNumber': cardNumber,
      'samData': samData,
      'mode': mode
    });

    if (revResult == "" || revResult == "REVERSAL_DONE") {
      return true;
    } else if (revResult.contains("Not Allowed Reversal")) {
      status[0] = "Not Allowed Reversal";
      return false;
    } else if (revResult.contains("Reversal Failed") ||
        revResult.contains("FAILED_REVERSAL") ||
        revResult.contains("Something Went Wrong")) {
      status[0] = "Reversal Failed";
      return false;
    } else {
      if (revResult.contains("NEW")) {
        List<String> items = revResult.split(",");
        String rApduSamCommand = await FlutterNfcKit.transceive(items[1]);
        String rApduCert = await FlutterNfcKit.transceive(items[2]);
        String samData =
            rApduSamCommand.substring(0, rApduSamCommand.length - 4) +
                rApduCert.substring(0, rApduCert.length - 4);
        return _doReversalIfExist(cardNumber, samData, "REVERSAL_NEW", status);
      } else {
        String apdu = await FlutterNfcKit.transceive(revResult.toString());
        return _doReversalIfExist(cardNumber, apdu, "REVERSAL_LOOP", status);
      }
    }
  }

  static Future<bool> _confirmUpdate(
      String cardUid,
      String bankType,
      String cardNumber,
      String samData,
      List<String> status,
      String reffNo,
      String balance,
      String writeStatus,
      String email,
      String phoneNumber) async {
    String respConfirm =
        await _channel.invokeMethod('confirmUpdate', <String, dynamic>{
      'cardUid': cardUid,
      'bankType': bankType,
      'cardNumber': cardNumber,
      'samData': samData,
      'reff_no': reffNo,
      'balance': balance,
      'writeStatus': writeStatus,
      'email': email,
      'phoneNumber': phoneNumber
    });

    List<String> items = respConfirm.toString().split(",");
    if (items[0] == "" || items[0] == "N/A") {
      print("respConfirm ${items[1]}");
      status[0] = items[1];
      return true;
    } else if (items[0].contains("Success")) {
      status[0] = items[0];
      return true;
    } else if (items[0].contains("No Pending Balance") ||
        items[0].contains("Data Missmatch") ||
        items[0].contains("General Error") ||
        items[0].contains("Something went wrong")) {
      status[0] = items[0];
      return false;
    } else {
      String apdu = await FlutterNfcKit.transceive(items[0]);
      return _confirmUpdate(
          cardUid,
          bankType,
          cardNumber,
          apdu,
          status,
          reffNo,
          balance,
          _apduIsOke(apdu) && items[2] == "NEW" ? "COMPLETED" : "",
          email,
          phoneNumber);
    }
  }

  static Future<bool> getHistory(List<String> history) async {
    bool result = false;
    String apduClSelectApp = await _channel.invokeMethod('apduIsMyCard');
    String apduClBalance = await _channel.invokeMethod('apduBalance');
    var dataBalance = jsonDecode(apduClBalance);
    var dataSelect = jsonDecode(apduClSelectApp);
    List<ApduResponse> apduList = dataSelect
        .map<ApduResponse>((result) => new ApduResponse.fromJson(result))
        .toList();
    List<ApduResponse> apduListBalance = dataBalance
        .map<ApduResponse>((result) => new ApduResponse.fromJson(result))
        .toList();
    try {
      for (ApduResponse s in apduList) {
        String selectApp = await FlutterNfcKit.transceive(s.apdu);
        if (_apduIsOke(selectApp)) {
          for (ApduResponse b in apduListBalance) {
            if (s.bankType == b.bankType) {
              if (s.bankType == "MANDIRI") {
                await FlutterNfcKit.transceive(b.apdu);
                result = await _getHistoryMandiri(history);
              } else if (s.bankType == "BNI") {
                result = await _getHistoryBni(history);
              }
            }
          }
        }
      }
    } catch (error) {
      result = false;
    }
    return result;
  }

  static Future<bool> _getHistoryMandiri(List<String> history) async {
    String apduClCardInfo = await _channel.invokeMethod('apduCardInfo');
    String apduCardInfo = await FlutterNfcKit.transceive(apduClCardInfo);
    String apduHistory = await _channel.invokeMethod('getHistory',
        <String, dynamic>{'apduCardInfo': apduCardInfo, 'bankType': "MANDIRI"});
    ApduResponse apduResponse = ApduResponse.fromJson(jsonDecode(apduHistory));

    String letApdu = "";
    String bankType = "";

    for (var i = 0; i < 0xF; i++) {
      letApdu += await _doProcessHistoryMandiri(apduResponse, i);
    }

    if (apduResponse.bankType == "MANDIRI_OLD")
      bankType = "MANDIRI_OLD";
    else
      bankType = "MANDIRI_NEW";

    String parseHistory = await _channel.invokeMethod(
        'parsingHistory', <String, dynamic>{
      'bankType': bankType,
      'rApdu': letApdu.substring(0, letApdu.length - 1)
    });
    history[0] = parseHistory;
    return true;
  }

  static Future<String> _doProcessHistoryMandiri(
      ApduResponse apduResponse, dynamic index) async {
    String letApdu = "";
    var rawApduHis = Uint8List.fromList(hex.decode(apduResponse.apdu));
    rawApduHis[2] = index;
    String responseApdu =
        await FlutterNfcKit.transceive(hex.encode(rawApduHis).toUpperCase());
    if (_apduIsOke(responseApdu)) {
      letApdu += "$responseApdu,";
    }
    return letApdu;
  }

  static Future<bool> _getHistoryBni(List<String> history) async {
    String crn = await _channel.invokeMethod('apduCrn');
    String manipulate = await _channel.invokeMethod('apduManipulate');
    await FlutterNfcKit.transceive(crn);
    String apduCardInfo = await FlutterNfcKit.transceive(manipulate);
    String apduHistory = await _channel.invokeMethod('getHistory',
        <String, dynamic>{'apduCardInfo': apduCardInfo, 'bankType': "BNI"});
    ApduResponse rApdu = ApduResponse.fromJson(jsonDecode(apduHistory));

    String letApdu = "";
    List<int> apduHis = hex.decode(rApdu.apdu);

    for (int i = 0; i < 0xF; i++) {
      apduHis[5] = i;
      String apduResponse =
          await FlutterNfcKit.transceive(hex.encode(apduHis).toUpperCase());
      if (_apduIsOke(apduResponse)) {
        letApdu += "$apduResponse,";
      }
    }
    String parseHistory = await _channel.invokeMethod(
        'parsingHistory', <String, dynamic>{
      'bankType': "BNI",
      'rApdu': letApdu.substring(0, letApdu.length - 1)
    });
    history[0] = parseHistory;
    return true;
  }

  static bool _apduIsOke(String apdu) {
    String result = apdu.substring(apdu.length - 4, apdu.length);
    return result == "9000";
  }

  static Future<void> stopReader(
      {String messageError = "", String messageSuccess = ""}) async {
    if (messageSuccess != "") {
      await FlutterNfcKit.finish(iosAlertMessage: messageSuccess);
    } else if (messageError != "") {
      await FlutterNfcKit.finish(iosErrorMessage: messageError);
    } else {
      await FlutterNfcKit.finish();
    }
  }

  static Future<void> setIosMessage(String message) async {
    await FlutterNfcKit.setIosAlertMessage(message);
  }

  static Future<String> getEncrypt(String plainText) async {
    String encryptedText = await _channel
        .invokeMethod('encryptGCM', <String, dynamic>{'message': plainText});
    return encryptedText;
  }

  static Future<String> getDecrypt(String encryptedText) async {
    String plainText = await _channel.invokeMethod(
        'decryptGCM', <String, dynamic>{'message': encryptedText});
    return plainText;
  }
}
