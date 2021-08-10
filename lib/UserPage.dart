import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_sound/flutter_sound.dart';
import 'package:flutter_sound_platform_interface/flutter_sound_recorder_platform_interface.dart';
import 'package:logger/logger.dart' show Level;
import 'package:permission_handler/permission_handler.dart';

import 'Audio.dart';

typedef Fn = void Function();

class UserPage extends StatefulWidget {
  final String title;

  UserPage({required this.title}) : super();

  @override
  _UserPageState createState() => _UserPageState();
}

class _UserPageState extends State<UserPage> with TickerProviderStateMixin {
  FlutterSoundPlayer? _player = FlutterSoundPlayer(logLevel: Level.debug);
  bool _playerIsInited = false;
  FlutterSoundRecorder? _recorder = FlutterSoundRecorder(logLevel: Level.debug);
  bool _recorderIsInited = false;
  bool _playbackReady = false;
  final String _audioPrefix = 'Audio_';

  List<Uint8List> sinkData = <Uint8List>[];

  // ignore: cancel_subscriptions
  StreamSubscription? _mRecordingDataSubscription;

  final _audios = <Audio>[];
  int _playingIndex = -1;

  @override
  void initState() {
    super.initState();

    _open();
  }

  @override
  void dispose() {
    // Be careful : you must `close` the audio session when you have finished with it.
    _player!.closeAudioSession();
    _player = null;

    _recorder!.closeAudioSession();
    _recorder = null;

    super.dispose();
  }

  Future<void> openTheRecorder() async {
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

    // Be careful : openAudioSession returns a Future.
    // Do not access your FlutterSoundPlayer or FlutterSoundRecorder before the completion of the Future
    await _player!.openAudioSession(
      device: AudioDevice.speaker,
      audioFlags: outputToSpeaker | allowHeadset | allowEarPiece | allowBlueToothA2DP,
    );

    setState(() {
      _playerIsInited = true;
    });

    await openTheRecorder();
    setState(() {
      _recorderIsInited = true;
    });
  }

  String getAudioTitle(index) => _audioPrefix + (index + 1).toString();

  void playAudio() async {
    playAudioAtIndex(_audios.length - 1);
  }

  static List<int> _reduce(List<Uint8List> a) {
    final arr = <int>[];
    a.reduce((value, element) {
      arr.addAll(value);
      return element;
    });

    return arr;
  }

  Future<void> playAudioAtIndex(int index) async {
    assert(_playerIsInited && _playbackReady && _recorder!.isStopped && _player!.isStopped && _audios.length > 0 && index < _audios.length);

    setState(() {
      _playingIndex = index;
    });

    final allData = Uint8List.fromList(_audios[index].audioData);

    await _player!.startPlayer(
      fromDataBuffer: allData,
      codec: Codec.pcm16,
      whenFinished: () {
        setState(() {
          _playingIndex = -1;
        });
      },
    );
  }

  Future<void> stopPlayer() async {
    await _player!.stopPlayer();
  }

  void recordAudio() async {
    if (!_recorder!.isStopped) {
      return;
    }

    setState(() {
      _playbackReady = false;
    });

    // ignore: close_sinks
    var recordingDataController = StreamController<Food>();

    _mRecordingDataSubscription = recordingDataController.stream.listen((buffer) {
      if (buffer is FoodData) {
        sinkData.add(buffer.data!);
      }
    });

    await _recorder!.startRecorder(
      // toFile: file,
      toStream: recordingDataController.sink,
      codec: Codec.pcm16,
      audioSource: AudioSource.microphone,
    );
  }

  void stopRecorder() async {
    await _recorder!.stopRecorder();

    if (_mRecordingDataSubscription != null) {
      await _mRecordingDataSubscription!.cancel();
    }

    setState(() {
      _playbackReady = true;
    });

    // final file = getFileName();
    _audios.add(Audio()..audioData = _reduce(sinkData));
    sinkData = <Uint8List>[];
  }

  //------------------------

  Fn? getRecorderFn() {
    if (!_recorderIsInited || !_player!.isStopped) {
      return null;
    }

    return _recorder!.isStopped ? recordAudio : stopRecorder;
  }

  Fn? getPlaybackFn() {
    if (!_playerIsInited || !_recorder!.isStopped || _audios.length == 0) {
      return null;
    }

    return _player!.isStopped && _audios.length > 0 ? playAudio : stopPlayer;
  }

  Future<void> platAudioFileAtIndex(int index) async {
    if (!_player!.isStopped) {
      await stopPlayer();
    }

    if (_playingIndex != index) {
      playAudioAtIndex(index);
    } else {
      setState(() {
        _playingIndex = -1;
      });
    }
  }

  Future<void> sendFile(Audio audio) async {
    audio.sent = true;

    Socket? socket;
    try {
      socket = await Socket.connect(widget.title, 38513);
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

  //------------------

  Widget makeBody() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: [
        Expanded(
          child: DecoratedBox(
            decoration: BoxDecoration(
              gradient: RadialGradient(
                center: Alignment(-0, -0),
                radius: 0.3,
                colors: <Color>[
                  Color(0xFFBBBBBB),
                  Color(0xFFAAAAAA),
                ],
                stops: <double>[1, 1],
              ),
            ),
            child: ListView.builder(
              itemCount: _audios.length,
              itemBuilder: (context, index) => Card(
                child: ListTile(
                  leading: Icon(Icons.music_note_rounded),
                  title: Text(getAudioTitle(index)),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        onPressed: () => platAudioFileAtIndex(index),
                        color: Colors.green,
                        icon: Icon(_playingIndex == index ? Icons.pause : Icons.play_arrow),
                      ),
                      IconButton(
                        onPressed: () async {
                          setState(() {
                            _audios.removeAt(index);
                          });
                        },
                        color: Colors.red.shade400,
                        icon: Icon(Icons.delete),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
        Center(
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                GestureDetector(
                  onTapDown: (_) => getRecorderFn()?.call(),
                  onTapUp: (_) => getRecorderFn()?.call(),
                  onPanEnd: (_) => getRecorderFn()?.call(),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(120),
                    child: ConstrainedBox(
                      constraints: BoxConstraints.tightFor(
                        width: 100,
                        height: 100,
                      ),
                      child: Container(
                        color: _recorder!.isStopped ? Colors.white : Colors.lime,
                        child: Icon(
                          _recorder!.isStopped ? Icons.mic_off : Icons.mic,
                          size: 40,
                        ),
                      ),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      ConstrainedBox(
                        constraints: BoxConstraints.tightFor(height: 60),
                        child: ElevatedButton(
                          onPressed: getPlaybackFn(),
                          child: Padding(
                            padding: const EdgeInsets.only(left: 8.0, right: 8),
                            child: Text(
                              !_player!.isStopped ? 'Stop' : 'Play Last Message',
                              style: TextStyle(
                                color: getPlaybackFn() != null ? Colors.black : Colors.grey,
                              ),
                            ),
                          ),
                          style: ButtonStyle(
                            backgroundColor: MaterialStateProperty.all<Color>(Colors.yellow.shade700),
                          ),
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.only(left: 8.0),
                        child: ConstrainedBox(
                          constraints: BoxConstraints.tightFor(width: 100, height: 60),
                          child: ElevatedButton(
                            onPressed: _audios.length > 0 && !_audios[_audios.length - 1].sent
                                ? () async {
                                    await sendFile(_audios[_audios.length - 1]);
                                  }
                                : null,
                            child: Text('Send'),
                            style: ButtonStyle(
                              backgroundColor: MaterialStateProperty.all<Color>(Colors.purple),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.cyan.shade800,
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
        backgroundColor: Colors.cyan.shade700,
      ),
      body: makeBody(),
    );
  }
}
