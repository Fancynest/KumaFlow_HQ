class ApduResponse {
  String bankType;
  String apdu;

  ApduResponse(this.bankType, this.apdu);

  Map<String, dynamic> toJson() =>
      {'bank_type': this.bankType, 'apdu': this.apdu};

  factory ApduResponse.fromJson(dynamic json) {
    return ApduResponse(json['bank_type'], json['apdu']);
  }
}


