import 'package:flutter/material.dart';

class TapCard extends StatefulWidget {
  @override
  _TapCardState createState() => _TapCardState();
}

class _TapCardState extends State<TapCard>{

  @override
  void initState() {

    super.initState();
  }

  @override
  dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
        padding: EdgeInsets.all(30),
        child: Column(mainAxisAlignment: MainAxisAlignment.start, children: [
          Image(
            image: AssetImage("images/gif_tap_card.gif"),
            width: 280,
          ),
          SizedBox(
            height: 20,
          ),
          ElevatedButton.icon(
            label: Text('Cancel'),
            icon: Icon(Icons.exit_to_app),
            onPressed: () {
              Navigator.of(context).pop();
            },
          )
        ]));
  }
}
