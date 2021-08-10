import 'dart:convert';

import 'package:flutter/widgets.dart';

class Device {
  String? name;
  String? ip;
  String? id;

  Device(this.name, this.ip, this.id);

  Device.fromJson(Map<String, dynamic> json)
      : name = json['name'],
        ip = json['ip'],
        id = json['id'];

  String toString() => "$name${String.fromCharCode(255)}$ip${String.fromCharCode(255)}$id";
}
