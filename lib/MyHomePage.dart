// import 'dart:convert';
// import 'dart:io';

import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:flutter/material.dart';
import 'package:network_info_plus/network_info_plus.dart';
import 'package:sorted_list/sorted_list.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_sound/flutter_sound.dart';
import 'package:flutter_sound_platform_interface/flutter_sound_recorder_platform_interface.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:logger/logger.dart' show Level;
import 'package:device_info_plus/device_info_plus.dart';

import 'Device.dart';
import 'Audio.dart';
import 'NsdDiscoverer.dart';

typedef Fn = void Function(int index);

class MyHomePage extends StatefulWidget {
  MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _model = "";
  String _ip = "";

  bool _busy = false, _show = false;
  final _devices = SortedList<Device>((a, b) {
    var n1 = 0;
    if (a.name != null) {
      n1 = int.parse(a.name!.substring(a.name!.lastIndexOf('.') + 1));
    }

    var n2 = 0;
    if (b.name != null) {
      n2 = int.parse(b.name!.substring(b.name!.lastIndexOf('.') + 1));
    }

    return n1.compareTo(n2);
  });

  bool _playerIsInited = false;
  FlutterSoundRecorder? _recorder = FlutterSoundRecorder(logLevel: Level.debug);
  bool _recorderIsInited = false;
  StreamSubscription? _mRecordingDataSubscription;
  List<Uint8List> sinkData = <Uint8List>[];
  Audio? _audio = null;

  @override
  initState() {
    super.initState();
    _getModel();
    _getIP();
    _open();
  }

  @override
  dispose() {
    super.dispose();
    NsdDiscoverer.stopDiscovering();
  }

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

    _discover();

    await Future.delayed(Duration(milliseconds: 3000));
    _stopScan();
  }

  void _stopScan() {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();

    NsdDiscoverer.stopDiscovering();

    setState(() {
      _busy = false;
    });
  }

  Future<void> _openTheRecorder() async {
    if (!kIsWeb) {
      var status = await Permission.microphone.request();
      if (status != PermissionStatus.granted) {
        throw RecordingPermissionException('Microphone permission not granted');
      }
    }
    await _recorder!.openAudioSession();
    _recorderIsInited = true;
  }

  Future<void> _open() async {
    var status = await Permission.microphone.request();
    if (status != PermissionStatus.granted) {
      throw RecordingPermissionException('Microphone permission not granted');
    }

    await _openTheRecorder();
    setState(() {
      _recorderIsInited = true;
    });
  }

  Map<int, bool> recording = {};

  void recordAudio(int index) async {
    if (!_recorder!.isStopped) {
      return;
    }

    // ignore: close_sinks
    var recordingDataController = StreamController<Food>();

    setState(() {
      recording[index] = true;
    });

    _mRecordingDataSubscription = recordingDataController.stream.listen((buffer) {
      if (buffer is FoodData) {
        sinkData.add(buffer.data!);
      }
    });

    print('tapped - recorder - playing');
    await _recorder!.startRecorder(
      // toFile: file,
      toStream: recordingDataController.sink,
      codec: Codec.pcm16,
      audioSource: AudioSource.microphone,
    );
  }

  void stopRecorder(int index) async {
    await _recorder!.stopRecorder();

    setState(() {
      recording[index] = false;
    });

    print('tapped - recorder - stopped');

    if (_mRecordingDataSubscription != null) {
      await _mRecordingDataSubscription!.cancel();
    }

    _audio = Audio()..audioData = _reduce(sinkData);
    _audio!.target = _devices[index];
    if (_audio!.audioData.isNotEmpty) {
      _sendFile(_audio!);
    }

    sinkData = <Uint8List>[];
  }

  static List<int> _reduce(List<Uint8List> a) {
    final arr = <int>[];
    a.reduce((value, element) {
      arr.addAll(value);
      return element;
    });

    return arr;
  }

  Fn? getRecorderFn(int index) {
    print('tapped - getRecorderFn');

    if (!_recorderIsInited) {
      return null;
    }

    return recording[index] == false ? recordAudio : stopRecorder;
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
        final bytes = audio.audioData;
        print("Original bytes length: ${bytes.length}");

        var gzipBytes = GZipEncoder().encode(bytes, level: Deflate.BEST_SPEED);
        if (gzipBytes != null) {
          print("After GZip, bytes length: ${gzipBytes.length}");

          var b = <int>[];
          b.add(deviceBytes.length);
          b.addAll(deviceBytes);
          b.addAll(gzipBytes);

          socket.listen((List<int> event) {
            if (event.length > 0) {
              print("Response: ${utf8.decode(event)}");
            }
          });

          print("Sending ${b.length} bytes");
          socket.add(b);
        }
      } catch (err) {
        print("Error: " + err.toString());
        audio.sent = false;
      } finally {
        socket.close();
      }
    }
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

  void _discover() {
    NsdDiscoverer.findAsync(_ip, (host, added) {
      if (added == true) {
        setState(() {
          recording[_devices.length] = false;
          _devices.add(Device(host[0], host[1], host[2]));
        });

        print('Found device: $host');
      } else {
        int index = _devices.indexWhere((d) => d.name == host[0]);
        if (index > -1) {
          setState(() {
            recording.removeWhere((key, value) => key == index);
            _devices.removeAt(index);
          });

          print('Lost device: $host');
        }
      }
    });
  }

  Future _getModel() async {
    AndroidDeviceInfo androidInfo = await DeviceInfoPlugin().androidInfo;
    _model = androidInfo.model ?? androidInfo.brand ?? "";
  }
}
