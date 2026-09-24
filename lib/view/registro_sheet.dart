import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:nutri/app/dependencies.dart';
import 'package:nutri/cubit/registro_cubit.dart';
import 'package:nutri/data/estimate.dart';

Future<void> abrirRegistro(BuildContext context) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (context) {
      return Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.viewInsetsOf(context).bottom,
        ),
        child: BlocProvider(
          create: (_) => getIt<RegistroCubit>(),
          child: const RegistroSheet(),
        ),
      );
    },
  );
}

class RegistroSheet extends StatefulWidget {
  const RegistroSheet({super.key});

  @override
  State<RegistroSheet> createState() => _RegistroSheetState();
}

class _RegistroSheetState extends State<RegistroSheet> {
  late final TextEditingController _texto;

  @override
  void initState() {
    super.initState();
    _texto = TextEditingController();
  }

  @override
  void dispose() {
    _texto.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RegistroCubit, RegistroState>(
      builder: (context, state) {
        final estimate = state.estimate;
        if (state.fase == RegistroFase.card && estimate != null) {
          return _CardCurto(estimate: estimate);
        }
        final cubit = context.read<RegistroCubit>();
        return SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.outlineVariant,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                const Text('Registrar'),
                const SizedBox(height: 12),
                TextField(
                  key: const Key('registro-texto'),
                  controller: _texto,
                  minLines: 2,
                  maxLines: 4,
                  decoration: const InputDecoration(
                    hintText: '2 pães, manteiga, ovo, café com leite…',
                  ),
                  onChanged: cubit.texto,
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    OutlinedButton.icon(
                      key: const Key('escolher-foto'),
                      onPressed: cubit.escolherFoto,
                      icon: const Icon(Icons.photo_camera_outlined),
                      label: Text(state.foto == null ? 'Foto' : 'Foto anexada'),
                    ),
                  ],
                ),
                if (state.erro != null) ...[
                  const SizedBox(height: 12),
                  Text(state.erro!),
                ],
                const SizedBox(height: 20),
                FilledButton(
                  onPressed: state.fase == RegistroFase.enviando
                      ? null
                      : cubit.registrar,
                  child: Text(
                    state.fase == RegistroFase.enviando
                        ? 'Estimando…'
                        : 'Registrar',
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _CardCurto extends StatelessWidget {
  const _CardCurto({required this.estimate});

  final Estimate estimate;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Café · dia 1'),
            const SizedBox(height: 8),
            Text(
              '≈ ${estimate.kcal} kcal',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 4),
            Text('P ${estimate.p} · C ${estimate.c} · G ${estimate.g}'),
            const SizedBox(height: 8),
            Text('confiança ${estimate.confianca}'),
            if (estimate.mostraPergunta) ...[
              const SizedBox(height: 12),
              Text('1 pergunta · ${estimate.pergunta}'),
            ],
            const SizedBox(height: 8),
            const Text('Estimativa, não é consulta.'),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Confirmar'),
            ),
          ],
        ),
      ),
    );
  }
}
