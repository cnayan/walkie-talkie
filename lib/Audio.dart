import 'package:walkie_talkie/Device.dart';

class Audio {
  bool isMine = true;
  late List<int> audioData;
  bool sent = false;
  late Device target;
}
