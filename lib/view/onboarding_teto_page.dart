import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'package:nutri/cubit/perfil_cubit.dart';

class OnboardingTetoPage extends StatelessWidget {
  const OnboardingTetoPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: BlocBuilder<PerfilCubit, PerfilState>(
          builder: (context, state) {
            return ListView(
              padding: const EdgeInsets.all(24),
              children: [
                const Text('Primeiro open'),
                const SizedBox(height: 8),
                Text(
                  'Qual é o teto de kcal?',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 8),
                const Text('Você manda no número.'),
                const SizedBox(height: 20),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _ModoChip(
                      label: 'Mesmo todos os dias',
                      selecionado: state.modo == ModoTeto.mesmo,
                      onTap: () =>
                          context.read<PerfilCubit>().modo(ModoTeto.mesmo),
                    ),
                    _ModoChip(
                      label: 'Seg–sex / sáb–dom',
                      selecionado: state.modo == ModoTeto.utilFds,
                      onTap: () =>
                          context.read<PerfilCubit>().modo(ModoTeto.utilFds),
                    ),
                    _ModoChip(
                      label: 'Cada dia diferente',
                      selecionado: state.modo == ModoTeto.seteDias,
                      onTap: () =>
                          context.read<PerfilCubit>().modo(ModoTeto.seteDias),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                _CamposTeto(state: state),
                const SizedBox(height: 16),
                const Text(
                  'Dá pra mudar depois em 2 toques. Isso não é cálculo de nutricionista.',
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () => context.go('/onboarding/treino'),
                  child: const Text('Continuar'),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _ModoChip extends StatelessWidget {
  const _ModoChip({
    required this.label,
    required this.selecionado,
    required this.onTap,
  });

  final String label;
  final bool selecionado;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      label: Text(label),
      selected: selecionado,
      onSelected: (_) => onTap(),
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
          const Text('Cada dia'),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (var i = 0; i < 7; i++) Text('${_dias[i]} ${state.dias[i]}'),
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
        Text(widget.label),
        const SizedBox(height: 6),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _controller,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                onChanged: (texto) {
                  final kcal = int.tryParse(texto);
                  if (kcal != null) {
                    widget.onChanged(kcal);
                  }
                },
              ),
            ),
            const SizedBox(width: 8),
            Text(widget.sufixo),
          ],
        ),
      ],
    );
  }
}
