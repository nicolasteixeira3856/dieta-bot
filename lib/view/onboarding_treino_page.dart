import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'package:nutri/cubit/perfil_cubit.dart';

/// Preview do wire: treino de exemplo 480 kcal. O percentual digitado entra inteiro,
/// sem teto de +300 kcal.
int creditoPreview(int percentual) => (480 * percentual / 100).round();

class OnboardingTreinoPage extends StatefulWidget {
  const OnboardingTreinoPage({super.key});

  @override
  State<OnboardingTreinoPage> createState() => _OnboardingTreinoPageState();
}

class _OnboardingTreinoPageState extends State<OnboardingTreinoPage> {
  late final TextEditingController _percentual;

  @override
  void initState() {
    super.initState();
    _percentual = TextEditingController(
      text: '${context.read<PerfilCubit>().state.percentual}',
    );
  }

  @override
  void dispose() {
    _percentual.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: BlocBuilder<PerfilCubit, PerfilState>(
          builder: (context, state) {
            final cubit = context.read<PerfilCubit>();
            final credito = creditoPreview(state.percentual);
            return ListView(
              padding: const EdgeInsets.all(24),
              children: [
                const Text('Primeiro open'),
                const SizedBox(height: 8),
                Text(
                  'Quando você treina, o teto sobe?',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 20),
                _Opcao(
                  selecionada: state.politica == PoliticaEatBack.zero,
                  onTap: () => cubit.politica(PoliticaEatBack.zero),
                  titulo: 'Não entra · 0%',
                  detalhe: 'Treino 1000 e o teto continua o mesmo.',
                ),
                const SizedBox(height: 12),
                _Opcao(
                  selecionada: state.politica == PoliticaEatBack.parcial,
                  onTap: () => cubit.politica(PoliticaEatBack.parcial),
                  titulo: 'Entra um pouco',
                  detalhe: 'Você escolhe a %.',
                  extra: state.politica == PoliticaEatBack.parcial
                      ? Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const SizedBox(height: 10),
                            Row(
                              children: [
                                SizedBox(
                                  width: 88,
                                  child: TextField(
                                    key: const Key('percentual-eatback'),
                                    controller: _percentual,
                                    keyboardType: TextInputType.number,
                                    inputFormatters: [
                                      FilteringTextInputFormatter.digitsOnly,
                                    ],
                                    onChanged: (texto) {
                                      final valor = int.tryParse(texto);
                                      if (valor != null) {
                                        cubit.percentual(valor);
                                      }
                                    },
                                  ),
                                ),
                                const SizedBox(width: 8),
                                const Text('% que entra'),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Preview: treino 480 → teto +$credito · hoje ${state.kcalMesmo + credito}',
                            ),
                          ],
                        )
                      : null,
                ),
                const SizedBox(height: 12),
                _Opcao(
                  selecionada: state.politica == PoliticaEatBack.cem,
                  onTap: () => cubit.politica(PoliticaEatBack.cem),
                  titulo: 'Entra tudo · 100%',
                  detalhe: 'Treino 480 → teto +480.',
                  extra: const Padding(
                    padding: EdgeInsets.only(top: 8),
                    child: Text(
                      'Pulseira costuma superestimar. O número que você digitar entra inteiro.',
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                const Text('Não é recomendação clínica. É regra sua.'),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () => context.go('/'),
                  child: const Text('Entrar no app'),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _Opcao extends StatelessWidget {
  const _Opcao({
    required this.selecionada,
    required this.onTap,
    required this.titulo,
    required this.detalhe,
    this.extra,
  });

  final bool selecionada;
  final VoidCallback onTap;
  final String titulo;
  final String detalhe;
  final Widget? extra;

  @override
  Widget build(BuildContext context) {
    final cor = selecionada
        ? Theme.of(context).colorScheme.primary
        : Theme.of(context).colorScheme.outline;
    return Material(
      color: selecionada
          ? Theme.of(context).colorScheme.primaryContainer
          : null,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: cor),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(titulo, style: const TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Text(detalhe),
              ?extra,
            ],
          ),
        ),
      ),
    );
  }
}
