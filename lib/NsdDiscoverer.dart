import 'dart:typed_data';

import 'package:bonsoir/bonsoir.dart';

// import 'package:multicast_dns/multicast_dns.dart';

class NsdDiscoverer {
  static BonsoirDiscovery? _discovery;
  static var splitter = String.fromCharCode(255);

  static findAsync(ip, void Function(List<String>, bool) callback) async {
    if (_discovery != null) return;
    _discovery = BonsoirDiscovery(type: "_cnayan_walkie_talkie._udp");

    // This is the type of service we're looking for..
    // Once defined, we can start the discovery :
    await _discovery!.ready;
    await _discovery!.start();

    // If you want to listen to the discovery :
    _discovery?.eventStream!.listen((event) {
      var name = event.service?.name;
      var type = event.type;

      if (name != null) {
        Future(() {
          var a = name.split(splitter);
          if (ip != a[1]) {
            if (type == BonsoirDiscoveryEventType.DISCOVERY_SERVICE_RESOLVED) {
              callback.call(a, true);
            } else if (type == BonsoirDiscoveryEventType.DISCOVERY_SERVICE_LOST) {
              callback.call(a, false);
            }
          }
        });
      }
    });
  }

  static stopDiscovering() async {
    // Then if you want to stop the discovery :
    await _discovery?.stop();
    _discovery = null;
  }
}
