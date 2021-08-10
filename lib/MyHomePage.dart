import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:network_info_plus/network_info_plus.dart';
import 'package:sorted_list/sorted_list.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:device_info_plus/device_info_plus.dart';

import 'Device.dart';
import 'Audio.dart';
import 'Utils.dart';

typedef Fn = void Function(int index);

class MyHomePage extends StatefulWidget {
  MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const _mnsdMethodChannel = MethodChannel('com.cnayan.walkie-talkie/nsd-publish');
  static const _recordingMethodChannel = MethodChannel('com.cnayan.walkie-talkie/audio-publish');
  String _model = "";
  String _ip = "";

  bool _busy = false, _show = false;
  final _devices = SortedList<Device>((a, b) => a.name!.compareTo(b.name!));

  var isRecording = false;
  Map<int, bool> recording = {};
  bool _recorderIsInited = false;
  List<Uint8List> sinkData = <Uint8List>[];
  Audio? _audio;

  @override
  initState() {
    super.initState();
    _getModel();
    _getIP();
    _open();

    _setupAudioRecorder();

    Future.delayed(Duration(seconds: 1)).then((value) => _scanLAN());
  }

  // @override
  // dispose() {
  //   super.dispose();
  // }

  void _getIP() async {
    final NetworkInfo ni = NetworkInfo();
    var ip = await ni.getWifiIP();
    if (ip == null) {
      throw NullThrownError();
    }

    setState(() {
      _ip = ip;
    });
  }

  void _scanLAN() async {
    setState(() {
      _show = true;
      _busy = true;
      _devices.clear();
    });

    var devices = await _getNetworkDevices();
    if (devices != null) {
      setState(() {
        _devices.clear();
        _devices.addAll(devices);
      });
    }

    _stopScan();
  }

  Future _stopScan() async {
    setState(() {
      _busy = false;
    });
  }

  // Future<void> _openTheRecorder() async {
  //   if (!kIsWeb) {
  //     var status = await Permission.microphone.request();
  //     if (status != PermissionStatus.granted) {
  //       throw RecordingPermissionException('Microphone permission not granted');
  //     }
  //   }
  //   await _recorder!.openAudioSession();
  //   _recorderIsInited = true;
  // }

  Future<void> _open() async {
    var status = await Permission.microphone.request();
    if (status != PermissionStatus.granted) {
      throw ErrorDescription('Microphone permission not granted');
    }

    _setupAudioRecorder();
    setState(() {
      _recorderIsInited = true;
    });
  }

  void _setupAudioRecorder() {
    _recordingMethodChannel.setMethodCallHandler((MethodCall call) async {
      if (call.method == "audioBytes") {
        print("Got audioBytes: ${call.arguments.length}");
        Uint8List arr = Uint8List.fromList(call.arguments);
        sinkData.add(arr);
      }
    });
  }

  void recordAudio(int index) async {
    print('tapped - recorder - recording');

    if (isRecording) {
      return;
    }

    await _recordingMethodChannel.invokeMethod("startRecording");

    setState(() {
      recording[index] = true;
    });
  }

  void stopRecorder(int index) async {
    print('tapped - recorder - stopped');
    await _recordingMethodChannel.invokeMethod("stopRecording");

    setState(() {
      recording[index] = false;
    });

    _audio = Audio()..audioData = Utils.reduce(sinkData);
    _audio!.target = _devices[index];
    if (_audio!.audioData.isNotEmpty) {
      _sendFile(_audio!);
    }

    sinkData.clear();
  }

  Fn? getRecorderFn(int index) {
    if (!_recorderIsInited) {
      return null;
    }

    return recording[index] == null || recording[index] == false ? recordAudio : stopRecorder;
  }

  Future<void> _sendFile(Audio audio) async {
    audio.sent = true;

    var device = Device(_model, _ip, "");
    var deviceStr = device.toString();
    var deviceBytes = utf8.encode(deviceStr);

    Socket? socket;
    try {
      socket = await Socket.connect(audio.target.ip!, 38513);
      print('connected');
    } catch (err) {
      print("Error: " + err.toString());
      audio.sent = false;
    }

    if (socket != null) {
      try {
        var gzipBytes = _compressBytes(deviceBytes, audio.audioData);
        if (gzipBytes != null) {
          print("After compression, bytes length: ${gzipBytes.length}");

          socket.listen((List<int> event) {
            if (event.length > 0) {
              print("Response: ${utf8.decode(event)}");
            }
          });

          print("Sending ${gzipBytes.length} bytes");
          socket.add(gzipBytes);
        }
      } catch (err) {
        print("Error: " + err.toString());
        audio.sent = false;
      } finally {
        socket.close();
      }
    }
  }

  List<int>? _compressBytes(deviceBytes, audioData) {
    final bytes = <int>[];
    bytes.add(deviceBytes.length);
    bytes.addAll(deviceBytes);
    bytes.addAll(audioData);

    print("Before compression, total bytes length: ${bytes.length}");

    return GZipEncoder().encode(bytes, level: Deflate.BEST_SPEED);
  }

  // ====================================================================

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.

    return Scaffold(
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Row(
          children: [
            Text(widget.title),
            Spacer(),
            Text(
              'You: ',
              style: Theme.of(context).textTheme.subtitle1?.merge(TextStyle(color: Colors.white)),
            ),
            Text(
              '$_ip',
              style: Theme.of(context).textTheme.subtitle1?.merge(TextStyle(color: Colors.white)),
            ),
          ],
        ),
        backgroundColor: Colors.cyan.shade700,
      ),
      body: Container(
        margin: const EdgeInsets.only(bottom: 0),
        child: Column(
          children: [
            Container(
              color: Colors.cyan.shade800,
              height: 100,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Spacer(),
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      Text(
                        'Talk to your friends!',
                        style: TextStyle(color: Colors.white, fontSize: Theme.of(context).textTheme.headline6?.fontSize),
                      ),
                    ],
                  ),
                  Spacer(),
                  Image.asset(
                    'assets/images/aang.jpg',
                    width: 150,
                  ),
                ],
              ),
            ),
            Visibility(
              visible: _show,
              child: Expanded(
                child: Stack(
                  children: [
                    Visibility(
                      visible: _busy,
                      child: Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                CircularProgressIndicator(),
                                Padding(
                                  padding: const EdgeInsets.only(left: 8.0),
                                  child: Text(
                                    "Searching...",
                                    style: TextStyle(fontSize: Theme.of(context).textTheme.headline5?.fontSize),
                                  ),
                                ),
                              ],
                            ),
                            Padding(
                              padding: const EdgeInsets.all(8.0),
                              child: ConstrainedBox(
                                constraints: BoxConstraints.tightFor(width: 200),
                                child: ElevatedButton(
                                  style: ElevatedButton.styleFrom(
                                    textStyle: TextStyle(fontSize: Theme.of(context).textTheme.headline6?.fontSize),
                                  ),
                                  onPressed: _stopScan,
                                  child: Text(
                                    'Cancel',
                                  ),
                                ),
                              ),
                            ),
                            Padding(
                              padding: const EdgeInsets.all(8.0),
                              child: Text(
                                'Users found: ${_devices.length}',
                                style: TextStyle(fontSize: Theme.of(context).textTheme.bodyText1?.fontSize),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    Visibility(
                      visible: !_busy,
                      child: Center(
                        child: ListView.builder(
                          itemCount: _devices.length,
                          itemBuilder: (context, index) => Card(
                            child: GestureDetector(
                              onTapDown: (_) => getRecorderFn(index)?.call(index),
                              onTapUp: (_) => getRecorderFn(index)?.call(index),
                              onPanEnd: (_) => getRecorderFn(index)?.call(index),
                              child: ListTile(
                                tileColor: recording[index] == true ? Colors.lime : Colors.transparent,
                                leading: Icon(Icons.person),
                                title: Row(
                                  children: [
                                    Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          _devices[index].name!,
                                          style: TextStyle(fontSize: Theme.of(context).textTheme.headline6?.fontSize),
                                        ),
                                        Text(
                                          "(${_devices[index].ip!})",
                                          style: TextStyle(fontSize: Theme.of(context).textTheme.bodyText1?.fontSize),
                                        ),
                                      ],
                                    ),
                                    Spacer(),
                                    Container(
                                      child: Icon(
                                        recording[index] == true ? Icons.mic : Icons.mic_off,
                                        size: 20,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              // trailing: Row(
                              //   mainAxisSize: MainAxisSize.min,
                              //   children: [],
                              // ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: Container(
        color: Colors.cyan.shade800,
        height: 50,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ConstrainedBox(
              constraints: BoxConstraints.tightFor(width: 200, height: 40),
              child: ElevatedButton(
                onPressed: _busy ? null : _scanLAN,
                child: Text(
                  'Scan LAN',
                  style: TextStyle(color: _busy ? Colors.grey : Colors.white, fontSize: Theme.of(context).textTheme.headline6?.fontSize),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<List<Device>?> _getNetworkDevices() async {
    try {
      var deviceNames = await _mnsdMethodChannel.invokeMethod('getDevices');
      var devices = <Device>[];
      for (var deviceName in deviceNames) {
        var a = Utils.splitDeviceName(deviceName);
        if (_ip != a[1]) {
          devices.add(Device(a[0], a[1], a[2]));
        }
      }

      return devices;
    } on PlatformException catch (e) {
      print("Failed to interact with platform channel: '${e.message}'.");
    }

    return null;
  }

  Future _getModel() async {
    AndroidDeviceInfo androidInfo = await DeviceInfoPlugin().androidInfo;
    _model = androidInfo.model ?? androidInfo.brand ?? "";
  }
}
