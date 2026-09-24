import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nutri/app/nutri_theme.dart';
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

    final theme = Theme.of(tester.element(find.byType(Scaffold)));
    expect(theme.scaffoldBackgroundColor, nutriBg);
    expect(theme.colorScheme.primary, nutriGold);
    expect(theme.colorScheme.primary, isNot(const Color(0xFF1F6B4A)));
    final decoration =
        tester
                .widget<AnimatedContainer>(
                  find.ancestor(
                    of: find.text('Mesmo todos os dias'),
                    matching: find.byType(AnimatedContainer),
                  ),
                )
                .decoration!
            as BoxDecoration;
    expect((decoration.border! as Border).top.color, nutriGold);
  });
}
