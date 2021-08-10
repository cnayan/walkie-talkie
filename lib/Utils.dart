import 'dart:typed_data';

class Utils {
  static var splitter = String.fromCharCode(255);

  static splitDeviceName(String name) {
    return name.split(splitter);
  }

  static List<int> reduce(List<Uint8List> a) {
    final arr = <int>[];
    a.reduce((value, element) {
      arr.addAll(value);
      return element;
    });

    return arr;
  }
}
