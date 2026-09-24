import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:nutri/app/dependencies.dart';
import 'package:nutri/app/nutri_theme.dart';
import 'package:nutri/cubit/registro_cubit.dart';
import 'package:nutri/data/estimate.dart';

Future<void> abrirRegistro(BuildContext context) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: nutriSurface,
    constraints: const BoxConstraints(maxWidth: 430),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    sheetAnimationStyle: const AnimationStyle(
      duration: nutriMotion,
      reverseDuration: nutriMotion,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeOutCubic,
    ),
    builder: (context) {
      return Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
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
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const _Handle(),
                nutriKicker('Registrar'),
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
                Align(
                  alignment: Alignment.centerLeft,
                  child: OutlinedButton.icon(
                    key: const Key('escolher-foto'),
                    onPressed: cubit.escolherFoto,
                    icon: const Icon(Icons.photo_camera_outlined),
                    label: Text(state.foto == null ? 'Foto' : 'Foto anexada'),
                  ),
                ),
                if (state.erro != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    state.erro!,
                    style: const TextStyle(color: nutriError, fontSize: 13),
                  ),
                ],
                const SizedBox(height: 18),
                nutriCta(
                  state.fase == RegistroFase.enviando ? 'Estimando…' : 'Registrar',
                  state.fase == RegistroFase.enviando ? null : cubit.registrar,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _Handle extends StatelessWidget {
  const _Handle();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 40,
        height: 4,
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          color: const Color(0xFF3A424C),
          borderRadius: BorderRadius.circular(99),
        ),
      ),
    );
  }
}

class _CardCurto extends StatelessWidget {
  const _CardCurto({required this.estimate});

  final Estimate estimate;

  @override
  Widget build(BuildContext context) {
    final tokens = Theme.of(context).extension<NutriTokens>()!;
    final confianca = estimate.confianca.trim().toLowerCase();
    final cor = switch (confianca) {
      'alto' => tokens.ok,
      'baixo' => Theme.of(context).colorScheme.error,
      _ => Theme.of(context).colorScheme.primary,
    };
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const _Handle(),
            nutriKicker('Café · dia 1'),
            const SizedBox(height: 8),
            Text(
              '≈ ${estimate.kcal} kcal',
              style: const TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.w600,
                letterSpacing: -1.1,
                fontFeatures: [FontFeature.tabularFigures()],
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'P ${estimate.p} · C ${estimate.c} · G ${estimate.g}',
              style: const TextStyle(
                color: nutriMuted,
                fontSize: 13,
                fontFeatures: [FontFeature.tabularFigures()],
              ),
            ),
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: cor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(99),
                ),
                child: Text(
                  'confiança ${estimate.confianca}',
                  style: TextStyle(color: cor, fontSize: 11),
                ),
              ),
            ),
            if (estimate.mostraPergunta) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: nutriSurf2,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Text(
                  '1 pergunta · ${estimate.pergunta}',
                  style: const TextStyle(fontSize: 13, height: 1.4),
                ),
              ),
            ],
            const SizedBox(height: 8),
            const Text(
              'Estimativa, não é consulta.',
              style: TextStyle(color: nutriDim, fontSize: 12, height: 1.4),
            ),
            const SizedBox(height: 18),
            nutriCta('Confirmar', () => Navigator.of(context).pop()),
          ],
        ),
      ),
    );
  }
}
