import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:nutri/app/nutri_theme.dart';
import 'package:nutri/cubit/perfil_cubit.dart';
import 'package:nutri/view/registro_sheet.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: BlocBuilder<PerfilCubit, PerfilState>(
          builder: (context, state) {
            final teto = state.orcamentoDeHoje().tetoEfetivo;
            const saldo = TextStyle(
              fontFeatures: [FontFeature.tabularFigures()],
              color: nutriText,
              fontSize: 34,
              fontWeight: FontWeight.w600,
              letterSpacing: -1.2,
              height: 1.05,
            );
            return NutriColumn(
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        _dataCurta(DateTime.now()),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: nutriMuted, fontSize: 13),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Teto $teto ▾',
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        fontFeatures: [FontFeature.tabularFigures()],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Text.rich(
                  TextSpan(
                    style: saldo,
                    text: '0 ',
                    children: [
                      TextSpan(
                        text: '/ $teto kcal',
                        style: saldo.copyWith(
                          color: nutriMuted,
                          fontSize: 20,
                          fontWeight: FontWeight.w500,
                          letterSpacing: 0,
                        ),
                      ),
                    ],
                  ),
                  key: const Key('saldo'),
                  style: saldo,
                ),
                const SizedBox(height: 4),
                const Text(
                  'P 0 / 170 g',
                  style: TextStyle(
                    color: nutriMuted,
                    fontSize: 13,
                    fontFeatures: [FontFeature.tabularFigures()],
                  ),
                ),
                const SizedBox(height: 12),
                const _Barra(fator: 0.02),
                Card(
                  key: const Key('proxima-janela'),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Text(
                          'Próxima',
                          style: TextStyle(
                            fontSize: 11,
                            letterSpacing: 1.1,
                            fontWeight: FontWeight.w600,
                            color: nutriGold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Café · ~09:30',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          'Ainda sem atalho. Escreve ou manda foto — do jeito que você fala.',
                          style: TextStyle(
                            color: nutriMuted,
                            fontSize: 13,
                            height: 1.45,
                          ),
                        ),
                        const SizedBox(height: 12),
                        nutriCta('O que cabe agora', () {}),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  key: const Key('campo-refeicao'),
                  readOnly: true,
                  onTap: () => abrirRegistro(context),
                  decoration: const InputDecoration(
                    hintText: 'O que você comeu, ou uma foto',
                    suffixIcon: Icon(Icons.photo_camera_outlined),
                  ),
                ),
                const SizedBox(height: 8),
                const _Linha('Café', 'pendente', agora: true),
                const _Linha('Lanche manhã', 'pendente'),
                const _Linha('Almoço', 'pendente'),
                const _Linha('Lanche tarde', 'pendente'),
                const _Linha('Janta', 'pendente'),
                const _Linha('Ceia', 'pendente'),
                const SizedBox(height: 16),
                const Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Treino hoje',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(color: nutriMuted, fontSize: 13),
                      ),
                    ),
                    SizedBox(width: 8),
                    Text(
                      'anotar kcal',
                      style: TextStyle(color: Color(0xFF8BB8D8), fontSize: 13),
                    ),
                  ],
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _Barra extends StatelessWidget {
  const _Barra({required this.fator});

  final double fator;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 6,
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: const Color(0xFF232A31),
        borderRadius: BorderRadius.circular(99),
      ),
      alignment: Alignment.centerLeft,
      child: FractionallySizedBox(
        widthFactor: fator,
        child: Container(
          decoration: BoxDecoration(
            color: nutriGold,
            borderRadius: BorderRadius.circular(99),
          ),
        ),
      ),
    );
  }
}

class _Linha extends StatelessWidget {
  const _Linha(this.nome, this.status, {this.agora = false});

  final String nome;
  final String status;
  final bool agora;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: Color(0xFF1C2228))),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              nome,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
            ),
          ),
          Text(
            status,
            style: TextStyle(
              fontSize: 13,
              color: agora ? nutriGold : nutriMuted,
            ),
          ),
        ],
      ),
    );
  }
}

String _dataCurta(DateTime data) {
  const semanas = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
  const meses = [
    'jan',
    'fev',
    'mar',
    'abr',
    'mai',
    'jun',
    'jul',
    'ago',
    'set',
    'out',
    'nov',
    'dez',
  ];
  return '${semanas[data.weekday - 1]} ${data.day} ${meses[data.month - 1]}';
}
