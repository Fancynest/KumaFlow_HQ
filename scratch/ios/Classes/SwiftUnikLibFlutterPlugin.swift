import Flutter
import UIKit
import iosmddlib

public class SwiftUnikLibFlutterPlugin: NSObject, FlutterPlugin {
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "unik_lib_flutter", binaryMessenger: registrar.messenger())
        let instance = SwiftUnikLibFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
        let cardProcessor: MainCardProcessor = MainCardProcessor()
        
        if call.method == "apduIsMyCard" {
            result(cardProcessor.getApduIsMyCard())
        }else if call.method == "apduCardInfo" {
            result(cardProcessor.getApduMandiriCardInfo())
        }else if call.method == "apduBalance" {
            result(cardProcessor.getApduBalance())
        }else if call.method == "apduCardAttr" {
            result(cardProcessor.getApduCardAttr())
        }else if call.method == "apduCardCert" {
            result(cardProcessor.getApduMandiriCert())
        }else if call.method == "apduCrn" {
            result(cardProcessor.getApduCrn())
        }else if call.method == "apduManipulate" {
            result(cardProcessor.getApduManipulateBNI())
        }else if call.method == "getHistory" {
            let arguments = call.arguments as! [String: Any?]
            let apdu = arguments["apduCardInfo"] as! String
            let bankName = arguments["bankType"] as! String
            let apduObject = ApduObject(bankName: bankName, apdu: apdu)
            result(cardProcessor.getApduHistory(apduObject: apduObject))
            
            
        }else if call.method == "parsingHistory" {
            let arguments = call.arguments as! [String: Any?]
            let rApdu = arguments["rApdu"] as! String
            let bankName = arguments["bankType"] as! String
            let apduObject = ApduObject(bankName: bankName, apdu: rApdu)
            let jsonHistory = cardProcessor.getHistory(apduObject: apduObject)
            print("jsonHistory \(jsonHistory)")
            result(jsonHistory)
            
        }else if call.method == "parseBalance" {
            let arguments = call.arguments as! [String: Any?]
            let bankType = arguments["bankType"] as! String
            let apdu = arguments["apduBalance"] as! String
            let obj = ApduObject(bankName: bankType, apdu: apdu)
            result(cardProcessor.getBalance(apduObject: obj))
            
        }else if call.method == "updateBalance" {
            
            let arguments = call.arguments as! [String: Any?]
            let cardNumber = arguments["cardNumber"] as! String
            let bankType = arguments["bankType"] as! String
            let cardUid = arguments["cardUid"] as! String
            let balance = arguments["balance"] as! String
            let cardAttr = arguments["cardAttr"] as! String
            let respCardInfo = arguments["respCardInfo"] as! String
            let email = arguments["email"] as! String
            let phoneNumber = arguments["phoneNumber"] as! String
            let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? ""
            
            let approvalCode = String(Int(NSDate().timeIntervalSince1970))
            
            PreferencesHelper.setDataWithSameKey(key: "APPCODE\(cardNumber)", value: approvalCode)
            
            let apduObject = ApduObject(bankName: bankType, apdu: "")
            let updateBalanceRequest = UpdateBalanceRequest(tid: deviceId, token: "82ae0ec8da26bca26d3540b08defc17d", mid: "49df5f81d59fae23ec6f542372fc9497", amount: "0", cardNo: cardNumber, cardAttribute: cardAttr, prevBalance: balance, lastBalance: balance, approvalCode: approvalCode, cardInfo: respCardInfo, cardUid: cardUid, samData: "", reffNo: approvalCode, phone: phoneNumber, email: email)
            
            cardProcessor.updateBalance(apduObject: apduObject, updateBalanceRequest: updateBalanceRequest) {data in
                
                print("Data update \(data)")
                
                result(data)
                
            }
            
            
            
        }else if call.method == "confirmUpdate" {
            
            let arguments = call.arguments as! [String: Any?]
            let cardUid = arguments["cardUid"] as! String
            let bankType = arguments["bankType"] as! String
            let cardNumber = arguments["cardNumber"] as! String
            let samData = arguments["samData"] as! String
            let reffNo = arguments["reff_no"] as! String
            let balance = arguments["balance"] as! String
            let writeStatus = arguments["writeStatus"] as! String
            let email = arguments["email"] as! String
            let phoneNumber = arguments["phoneNumber"] as! String
            let approvalCode = PreferencesHelper.getDataWithKey(key: "APPCODE\(cardNumber)")
            let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? ""
            
            let apduObject = ApduObject(bankName: bankType, apdu: cardUid)
            let confirmBalanceRequest = ConfirmBalanceRequest(tid: deviceId, token: "82ae0ec8da26bca26d3540b08defc17d", mid: "49df5f81d59fae23ec6f542372fc9497", cardNo: cardNumber, samData: samData, phone: phoneNumber, email: email, approvalCode: approvalCode, reffNo: reffNo, lastBalance: balance, writeStatus: writeStatus)
            
            if writeStatus == "COMPLETED" {
                PreferencesHelper.clearDataWithKey(key: "APPCODE\(cardNumber)")
            }
            
            cardProcessor.confirmUpdate(apduObject: apduObject, confirmBalanceRequest: confirmBalanceRequest) {data in
                
                result(data)
                
            }
            
            
        }else if call.method == "checkingReversal" {
            
            let arguments = call.arguments as! [String: Any?]
            let cardNumber = arguments["cardNumber"] as! String
            let samData = arguments["samData"] as! String
            let mode = arguments["mode"] as! String
            
            cardProcessor.reversalMandiri(cardNo: cardNumber, mode: mode, samData: samData) {data in
                
                result(data)
                
            }
                        
        }else if call.method == "composeBNI" {
            let arguments = call.arguments as! [String: Any?]
            let apduPurse = arguments["apduPurse"] as! String
            result(cardProcessor.getComposeBNI(cardInfo: apduPurse))
            
        }else if call.method == "createCrypto" {
            let arguments = call.arguments as! [String: Any?]
            let crypto = arguments["crypto"] as! String
            let rrn = arguments["rrn"] as! String
            
            result(cardProcessor.getCrypto(crypto: crypto, rrn: rrn))
            
        }else {
            result("iOS " + UIDevice.current.systemVersion)
        }
        
    }
    
    
}
