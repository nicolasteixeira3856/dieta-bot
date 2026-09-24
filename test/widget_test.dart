import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nutri/main.dart';

void main() {
  tearDown(() async {
    await getIt.reset();
  });

  testWidgets('abre o onboarding e não mostra o contador demo', (tester) async {
    await setupDependencies();
    await tester.pumpWidget(const NutriApp());
    await tester.pumpAndSettle();

    expect(find.text('Qual é o teto de kcal?'), findsOneWidget);
    expect(find.text('Mesmo todos os dias'), findsOneWidget);
    expect(find.byIcon(Icons.add), findsNothing);
    expect(
      find.text('You have pushed the button this many times:'),
      findsNothing,
    );
  });
}
