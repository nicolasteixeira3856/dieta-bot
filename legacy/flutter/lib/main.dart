import 'package:flutter/widgets.dart';

import 'package:nutri/app/dependencies.dart';
import 'package:nutri/app/nutri_app.dart';

export 'package:nutri/app/dependencies.dart';
export 'package:nutri/app/nutri_app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await setupDependencies();
  runApp(const NutriApp());
}
