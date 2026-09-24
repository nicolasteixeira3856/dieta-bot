import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'package:nutri/app/nutri_theme.dart';
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
            return NutriColumn(
              children: [
                nutriKicker('Primeiro open'),
                const SizedBox(height: 6),
                Text(
                  'Quando você treina, o teto sobe?',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 18),
                NutriOption(
                  selected: state.politica == PoliticaEatBack.zero,
                  onTap: () => cubit.politica(PoliticaEatBack.zero),
                  child: const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Não entra · 0%',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Treino 1000 e o teto continua o mesmo.',
                        style: TextStyle(color: nutriMuted, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                NutriOption(
                  selected: state.politica == PoliticaEatBack.parcial,
                  onTap: () => cubit.politica(PoliticaEatBack.parcial),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Entra um pouco',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Você escolhe a %.',
                        style: TextStyle(color: nutriMuted, fontSize: 13),
                      ),
                      if (state.politica == PoliticaEatBack.parcial) ...[
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
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontFeatures: [FontFeature.tabularFigures()],
                                ),
                                decoration: const InputDecoration(
                                  filled: true,
                                  fillColor: nutriBg,
                                  contentPadding: EdgeInsets.symmetric(
                                    horizontal: 10,
                                    vertical: 8,
                                  ),
                                ),
                                onChanged: (texto) {
                                  final valor = int.tryParse(texto);
                                  if (valor != null) {
                                    cubit.percentual(valor);
                                  }
                                },
                              ),
                            ),
                            const SizedBox(width: 8),
                            const Expanded(
                              child: Text(
                                '% que entra',
                                style: TextStyle(
                                  color: nutriMuted,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Preview: treino 480 → teto +$credito · hoje ${state.kcalMesmo + credito}',
                          style: const TextStyle(
                            color: nutriGold,
                            fontSize: 12,
                            height: 1.35,
                            fontFeatures: [FontFeature.tabularFigures()],
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                NutriOption(
                  selected: state.politica == PoliticaEatBack.cem,
                  onTap: () => cubit.politica(PoliticaEatBack.cem),
                  child: const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Entra tudo · 100%',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Treino 480 → teto +480.',
                        style: TextStyle(color: nutriMuted, fontSize: 13),
                      ),
                      SizedBox(height: 8),
                      Text(
                        'Pulseira costuma superestimar. O número que você digitar entra inteiro.',
                        style: TextStyle(
                          color: nutriGold,
                          fontSize: 12,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Não é recomendação clínica. É regra sua.',
                  style: TextStyle(color: nutriDim, fontSize: 12, height: 1.4),
                ),
                const SizedBox(height: 18),
                nutriCta('Entrar no app', () => context.go('/')),
              ],
            );
          },
        ),
      ),
    );
  }
}
