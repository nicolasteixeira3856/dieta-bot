import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:nutri/app/nutri_theme.dart';
import 'package:nutri/data/foto_picker.dart';
import 'package:nutri/main.dart';
import 'package:nutri/view/registro_sheet.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  tearDown(() async {
    await getIt.reset();
  });

  testWidgets('as três rotas abrem com os rótulos do wire', (tester) async {
    await _preparar(tester);
    expect(find.text('Qual é o teto de kcal?'), findsOneWidget);
    expect(find.text('Mesmo todos os dias'), findsOneWidget);
    expect(find.text('Seg–sex / sáb–dom'), findsOneWidget);
    expect(find.text('Cada dia diferente'), findsOneWidget);

    getIt<GoRouter>().go('/onboarding/treino');
    await tester.pumpAndSettle();
    expect(find.text('Quando você treina, o teto sobe?'), findsOneWidget);
    expect(find.text('Não entra · 0%'), findsOneWidget);
    expect(find.text('Entra um pouco'), findsOneWidget);
    expect(find.text('Entra tudo · 100%'), findsOneWidget);
    expect(find.text('Treino 480 → teto +480.'), findsOneWidget);

    getIt<GoRouter>().go('/');
    await tester.pumpAndSettle();
    expect(find.text('O que cabe agora'), findsOneWidget);
  });

  testWidgets('eat-back começa em 50 e não aplica cap de 300 kcal', (
    tester,
  ) async {
    await _preparar(tester);
    getIt<GoRouter>().go('/onboarding/treino');
    await tester.pumpAndSettle();

    await tester.tap(find.text('Entra um pouco'));
    await tester.pumpAndSettle();
    expect(find.text('50'), findsWidgets);
    expect(find.textContaining('teto +240'), findsOneWidget);
    expect(find.text('% que entra'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('percentual-eatback')), '80');
    await tester.pumpAndSettle();
    expect(find.textContaining('teto +384'), findsOneWidget);
    expect(find.textContaining('+300'), findsNothing);
    expect(find.textContaining('cap'), findsNothing);
  });

  testWidgets('home do dia 1 tem saldo, janela, campo e zero chips', (
    tester,
  ) async {
    await _preparar(tester);
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Entrar no app'));
    await tester.pumpAndSettle();

    expect(find.text('0 / 2000 kcal'), findsOneWidget);
    expect(find.text('P 0 / 170 g'), findsOneWidget);
    expect(find.text('Próxima'), findsOneWidget);
    expect(find.text('Café · ~09:30'), findsOneWidget);
    expect(
      find.text(
        'Ainda sem atalho. Escreve ou manda foto — do jeito que você fala.',
      ),
      findsOneWidget,
    );
    expect(find.text('O que você comeu, ou uma foto'), findsOneWidget);
    expect(find.byKey(const Key('campo-refeicao')), findsOneWidget);
    expect(find.text('O que cabe agora'), findsOneWidget);
    expect(find.byType(Chip), findsNothing);
    expect(find.byType(ChoiceChip), findsNothing);
    expect(find.byType(FilterChip), findsNothing);
    expect(find.byType(ActionChip), findsNothing);
    expect(find.byType(InputChip), findsNothing);
    expect(find.text('café de sempre'), findsNothing);
  });

  testWidgets('registrar manda o texto e a foto e mostra a pergunta', (
    tester,
  ) async {
    final bytes = Uint8List.fromList([9, 8, 7, 6]);
    final pedidos = <http.Request>[];
    await _preparar(
      tester,
      transport: _cliente(pedidos, {
        'kcal': 385,
        'p': 18,
        'c': 32,
        'g': 20,
        'confianca': 'medio',
        'pergunta': 'Foram 2 pães ou 1?',
        'itens': <Map<String, Object>>[],
        'model': 'gpt-6-luna',
      }),
      fotos: _FotoFixa(bytes),
    );
    getIt<GoRouter>().go('/');
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('campo-refeicao')));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('registro-texto')),
      '2 paes, ovo, cafe com leite',
    );
    await tester.tap(find.byKey(const Key('escolher-foto')));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Registrar'));
    await tester.pumpAndSettle();

    expect(pedidos, hasLength(1));
    final pedido = pedidos.single;
    expect(pedido.method, 'POST');
    expect(pedido.url.toString(), 'http://127.0.0.1:9/v1/estimate');
    final corpo = jsonDecode(pedido.body) as Map<String, dynamic>;
    expect(corpo['text'], '2 paes, ovo, cafe com leite');
    expect(base64Decode(corpo['image_b64'] as String), bytes);
    expect(pedido.headers['x-invite'], 'convite-teste');
    expect(find.textContaining('≈ 385 kcal'), findsOneWidget);
    expect(find.textContaining('Foram 2 pães ou 1?'), findsOneWidget);
    expect(find.textContaining('1 pergunta ·'), findsOneWidget);
  });

  testWidgets('confiança alta omite a pergunta', (tester) async {
    final pedidos = <http.Request>[];
    await _preparar(
      tester,
      transport: _cliente(pedidos, {
        'kcal': 200,
        'p': 10,
        'c': 20,
        'g': 5,
        'confianca': 'alto',
        'pergunta': 'nao deve aparecer',
        'itens': <Map<String, Object>>[],
        'model': 'gpt-6-luna',
      }),
    );
    getIt<GoRouter>().go('/');
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('campo-refeicao')));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('registro-texto')),
      'cafe puro',
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Registrar'));
    await tester.pumpAndSettle();

    expect(pedidos.single.method, 'POST');
    expect(find.textContaining('≈ 200 kcal'), findsOneWidget);
    expect(find.textContaining('nao deve aparecer'), findsNothing);
    expect(find.textContaining('1 pergunta'), findsNothing);
  });

  testWidgets('tema do wire, saldo tabular, sheet e zero chip no dia 1', (
    tester,
  ) async {
    await _preparar(tester);
    _expectTema(tester);
    _expectGold(tester, 'Mesmo todos os dias');

    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
    _expectTema(tester);
    _expectGold(tester, 'Não entra · 0%');
    expect(find.text('Entra um pouco'), findsOneWidget);
    expect(find.text('Entra tudo · 100%'), findsOneWidget);

    await tester.tap(find.text('Entrar no app'));
    await tester.pumpAndSettle();
    _expectTema(tester);
    final saldo = tester.widget<Text>(find.byKey(const Key('saldo')));
    expect(
      saldo.style?.fontFeatures,
      contains(const FontFeature.tabularFigures()),
    );
    expect(find.text('0 / 2000 kcal'), findsOneWidget);
    expect(find.text('O que cabe agora'), findsOneWidget);
    expect(find.byKey(const Key('campo-refeicao')), findsOneWidget);
    _expectZeroChips(tester);
    expect(find.byType(NavigationRail), findsNothing);

    await tester.tap(find.byKey(const Key('campo-refeicao')));
    await tester.pumpAndSettle();
    expect(find.byType(RegistroSheet), findsOneWidget);
    expect(find.byType(BottomSheet), findsOneWidget);
    expect(getIt<GoRouter>().state.uri.path, '/');
    _expectTema(tester);
  });

  testWidgets('390, 320 e a home larga não estouram nem viram trilho', (
    tester,
  ) async {
    final overflows = <String>[];
    final previous = FlutterError.onError;
    FlutterError.onError = (details) {
      final text = '${details.exceptionAsString()}\n$details';
      if (text.contains('RenderFlex overflowed') ||
          text.contains('A RenderFlex overflowed')) {
        overflows.add(text);
      }
      previous?.call(details);
    };
    addTearDown(() {
      FlutterError.onError = previous;
    });

    for (final size in const [Size(390, 844), Size(320, 640)]) {
      overflows.clear();
      await _percorrer(tester, size);
      expect(overflows, isEmpty, reason: '${size.width}x${size.height}');
      expect(tester.takeException(), isNull);
    }

    overflows.clear();
    await _percorrer(tester, const Size(1280, 800));
    expect(find.byType(NavigationRail), findsNothing);
    expect(find.byType(NutriColumn), findsWidgets);
    expect(find.byType(RegistroSheet), findsOneWidget);
    expect(getIt<GoRouter>().state.uri.path, '/');
    expect(overflows, isEmpty);
    expect(tester.takeException(), isNull);
  });

  test('fontes novas não carregam chave da OpenAI', () {
    final lib = Directory('lib');
    expect(lib.existsSync(), isTrue);
    for (final entidade in lib.listSync(recursive: true)) {
      if (entidade is! File || !entidade.path.endsWith('.dart')) {
        continue;
      }
      final texto = entidade.readAsStringSync();
      expect(texto.contains('OPENAI_API_KEY'), isFalse, reason: entidade.path);
      expect(texto.contains('sk-'), isFalse, reason: entidade.path);
    }
  });
}

void _expectTema(WidgetTester tester) {
  final theme = Theme.of(tester.element(find.byType(Scaffold).first));
  expect(theme.scaffoldBackgroundColor, nutriBg);
  expect(theme.colorScheme.primary, nutriGold);
  expect(theme.colorScheme.surface, nutriSurface);
  expect(theme.colorScheme.onSurface, nutriText);
  expect(theme.colorScheme.outline, nutriLine);
  expect(theme.colorScheme.error, nutriError);
  expect(theme.colorScheme.secondary, nutriOk);
  expect(theme.extension<NutriTokens>()?.muted, nutriMuted);
  expect(theme.colorScheme.primary, isNot(const Color(0xFF1F6B4A)));
}

void _expectGold(WidgetTester tester, String label) {
  final decoration =
      tester
              .widget<AnimatedContainer>(
                find.ancestor(
                  of: find.text(label),
                  matching: find.byType(AnimatedContainer),
                ),
              )
              .decoration!
          as BoxDecoration;
  expect((decoration.border! as Border).top.color, nutriGold);
}

void _expectZeroChips(WidgetTester tester) {
  expect(find.byType(Chip), findsNothing);
  expect(find.byType(ChoiceChip), findsNothing);
  expect(find.byType(FilterChip), findsNothing);
  expect(find.byType(ActionChip), findsNothing);
  expect(find.byType(InputChip), findsNothing);
}

Future<void> _rolarAte(WidgetTester tester, Finder finder) async {
  await tester.scrollUntilVisible(
    finder,
    80,
    scrollable: find.byType(Scrollable).first,
  );
  await tester.ensureVisible(finder);
  await tester.pumpAndSettle();
}

Future<void> _percorrer(WidgetTester tester, Size size) async {
  await tester.pumpWidget(const SizedBox.shrink());
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  await setupDependencies();
  await tester.pumpWidget(const NutriApp());
  await tester.pumpAndSettle();
  _expectTema(tester);

  await _rolarAte(tester, find.text('Continuar'));
  await tester.tap(find.text('Continuar'));
  await tester.pumpAndSettle();
  await _rolarAte(tester, find.text('Entra um pouco'));
  await tester.tap(find.text('Entra um pouco'));
  await tester.pumpAndSettle();
  expect(find.text('50'), findsWidgets);
  expect(find.textContaining('+300'), findsNothing);

  await _rolarAte(tester, find.text('Entrar no app'));
  await tester.tap(find.text('Entrar no app'));
  await tester.pumpAndSettle();
  expect(find.text('0 / 2000 kcal'), findsOneWidget);
  expect(find.text('O que cabe agora'), findsOneWidget);
  expect(find.byKey(const Key('proxima-janela')), findsOneWidget);
  _expectZeroChips(tester);
  expect(find.byType(NavigationRail), findsNothing);

  final saldo = tester.widget<Text>(find.byKey(const Key('saldo')));
  expect(
    saldo.style?.fontFeatures,
    contains(const FontFeature.tabularFigures()),
  );

  await _rolarAte(tester, find.byKey(const Key('campo-refeicao')));
  await tester.tap(find.byKey(const Key('campo-refeicao')));
  await tester.pumpAndSettle();
  expect(find.byType(BottomSheet), findsOneWidget);
  expect(getIt<GoRouter>().state.uri.path, '/');
}

Future<void> _preparar(
  WidgetTester tester, {
  http.Client? transport,
  FotoPicker? fotos,
}) async {
  tester.view.physicalSize = const Size(400, 900);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  await setupDependencies(transport: transport, fotos: fotos);
  await tester.pumpWidget(const NutriApp());
  await tester.pumpAndSettle();
}

http.Client _cliente(List<http.Request> pedidos, Map<String, Object?> corpo) {
  return MockClient((request) async {
    pedidos.add(request);
    return http.Response(jsonEncode(corpo), 200);
  });
}

class _FotoFixa implements FotoPicker {
  _FotoFixa(this.bytes);

  final Uint8List bytes;

  @override
  Future<Uint8List?> escolher() async => bytes;
}
