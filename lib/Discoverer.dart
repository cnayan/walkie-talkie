import 'dart:io' show RawDatagramSocket, RawSocketEvent, InternetAddress, Datagram;
import 'dart:convert' show utf8;

// import 'package:multicast_dns/multicast_dns.dart';

class Discoverer {
  static findAsync(ip, void Function(List<String> host) callback) async {
    // UDP client
    final datagramSocket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
    datagramSocket.broadcastEnabled = true;
    datagramSocket.readEventsEnabled = true;

    datagramSocket.listen((RawSocketEvent event) {
      if (event == RawSocketEvent.read) {
        Datagram? dg = datagramSocket.receive();
        if (dg != null) {
          var text = utf8.decode(dg.data);
          var a = text.split("!");
          if (ip != a[1]) {
            callback.call(a);
          }
          // print('Server: ${dg.address}:${dg.port} -- ${utf8.decode(dg.data)}');
          //datagramSocket.close();
        }
      }
    });

    datagramSocket.send("com.cnayan.walkietalkie-broadcast".codeUnits, InternetAddress("192.168.0.255"), 38512);
  }

  // static find() async {
  //   final factory = (dynamic host, int port, {bool reuseAddress = false, bool reusePort = false, int ttl = 1}) {
  //     return RawDatagramSocket.bind(host, port, reuseAddress: true, reusePort: false, ttl: ttl);
  //   };

  //   final client = MDnsClient(rawDatagramSocketFactory: factory);
  //   await client.start();

  //   const String name = '_walkie_talkie_udp.local';

  //   // Get the PTR recod for the service.
  //   await for (PtrResourceRecord ptr in client.lookup<PtrResourceRecord>(ResourceRecordQuery.addressIPv4(name).serverPointer(name))) {
  //     // Use the domainName from the PTR record to get the SRV record,
  //     // which will have the port and local hostname.
  //     // Note that duplicate messages may come through, especially if any
  //     // other mDNS queries are running elsewhere on the machine.
  //     await for (SrvResourceRecord srv in client.lookup<SrvResourceRecord>(ResourceRecordQuery.service(ptr.domainName))) {
  //       // Domain name will be something like "io.flutter.example@some-iphone.local._dartobservatory._tcp.local"
  //       final String bundleId = ptr.domainName; //.substring(0, ptr.domainName.indexOf('@'));
  //       print('Dart observatory instance found at '
  //           '${srv.target}:${srv.port} for "$bundleId".');
  //     }
  //   }

  //   client.stop();
  // }
}
