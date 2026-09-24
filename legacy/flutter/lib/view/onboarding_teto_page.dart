import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'package:nutri/app/nutri_theme.dart';
import 'package:nutri/cubit/perfil_cubit.dart';

class OnboardingTetoPage extends StatelessWidget {
  const OnboardingTetoPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: BlocBuilder<PerfilCubit, PerfilState>(
          builder: (context, state) {
            final cubit = context.read<PerfilCubit>();
            return NutriColumn(
              children: [
                nutriKicker('Primeiro open'),
                const SizedBox(height: 6),
                Text(
                  'Qual é o teto de kcal?',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  'Você manda no número.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 22),
                NutriOption(
                  selected: state.modo == ModoTeto.mesmo,
                  onTap: () => cubit.modo(ModoTeto.mesmo),
                  child: const Text('Mesmo todos os dias'),
                ),
                const SizedBox(height: 8),
                NutriOption(
                  selected: state.modo == ModoTeto.utilFds,
                  onTap: () => cubit.modo(ModoTeto.utilFds),
                  child: const Text('Seg–sex / sáb–dom'),
                ),
                const SizedBox(height: 8),
                NutriOption(
                  selected: state.modo == ModoTeto.seteDias,
                  onTap: () => cubit.modo(ModoTeto.seteDias),
                  child: const Text('Cada dia diferente'),
                ),
                const SizedBox(height: 18),
                AnimatedSwitcher(
                  duration: nutriMotion,
                  switchInCurve: Curves.easeOutCubic,
                  switchOutCurve: Curves.easeOutCubic,
                  child: KeyedSubtree(
                    key: ValueKey(state.modo),
                    child: _CamposTeto(state: state),
                  ),
                ),
                const SizedBox(height: 14),
                const Text(
                  'Dá pra mudar depois em 2 toques. Isso não é cálculo de nutricionista.',
                  style: TextStyle(color: nutriDim, fontSize: 12, height: 1.4),
                ),
                const SizedBox(height: 18),
                nutriCta('Continuar', () => context.go('/onboarding/treino')),
              ],
            );
          },
        ),
      ),
    );
  }
}

const _dias = ['S', 'T', 'Q', 'Q', 'S', 'S', 'D'];

class _CamposTeto extends StatelessWidget {
  const _CamposTeto({required this.state});

  final PerfilState state;

  @override
  Widget build(BuildContext context) {
    final cubit = context.read<PerfilCubit>();
    return switch (state.modo) {
      ModoTeto.mesmo => _Numero(
        label: 'Teto',
        sufixo: 'kcal/dia',
        valor: state.kcalMesmo,
        onChanged: cubit.kcalMesmo,
      ),
      ModoTeto.utilFds => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _Numero(
            label: 'Seg–sex',
            sufixo: 'kcal',
            valor: state.kcalUtil,
            onChanged: cubit.kcalUtil,
          ),
          const SizedBox(height: 12),
          _Numero(
            label: 'Sáb–dom',
            sufixo: 'kcal',
            valor: state.kcalFds,
            onChanged: cubit.kcalFds,
          ),
        ],
      ),
      ModoTeto.seteDias => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Cada dia',
            style: TextStyle(color: nutriMuted, fontSize: 12),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (var i = 0; i < 7; i++)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 7,
                  ),
                  decoration: BoxDecoration(
                    color: nutriSurf2,
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(color: nutriLine),
                  ),
                  child: Text(
                    '${_dias[i]} ${state.dias[i]}',
                    style: const TextStyle(
                      fontSize: 12,
                      fontFeatures: [FontFeature.tabularFigures()],
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    };
  }
}

class _Numero extends StatefulWidget {
  const _Numero({
    required this.label,
    required this.sufixo,
    required this.valor,
    required this.onChanged,
  });

  final String label;
  final String sufixo;
  final int valor;
  final ValueChanged<int> onChanged;

  @override
  State<_Numero> createState() => _NumeroState();
}

class _NumeroState extends State<_Numero> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: '${widget.valor}');
  }

  @override
  void didUpdateWidget(_Numero oldWidget) {
    super.didUpdateWidget(oldWidget);
    final texto = '${widget.valor}';
    if (oldWidget.valor != widget.valor && _controller.text != texto) {
      _controller.text = texto;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: const TextStyle(color: nutriMuted, fontSize: 12),
        ),
        const SizedBox(height: 6),
        TextField(
          controller: _controller,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          style: const TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.6,
            fontFeatures: [FontFeature.tabularFigures()],
          ),
          decoration: InputDecoration(suffixText: widget.sufixo),
          onChanged: (texto) {
            final kcal = int.tryParse(texto);
            if (kcal != null) {
              widget.onChanged(kcal);
            }
          },
        ),
      ],
    );
  }
}
