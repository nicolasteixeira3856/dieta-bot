import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

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
            return ListView(
              padding: const EdgeInsets.all(24),
              children: [
                Text(_dataCurta(DateTime.now())),
                Text('Teto $teto ▾'),
                const SizedBox(height: 16),
                Text(
                  '0 / $teto kcal',
                  key: const Key('saldo'),
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const Text('P 0 / 170 g'),
                const SizedBox(height: 20),
                Card(
                  key: const Key('proxima-janela'),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Próxima'),
                        const SizedBox(height: 4),
                        Text(
                          'Café · ~09:30',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          'Ainda sem atalho. Escreve ou manda foto — do jeito que você fala.',
                        ),
                        const SizedBox(height: 16),
                        FilledButton(
                          onPressed: () {},
                          child: const Text('O que cabe agora'),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                TextField(
                  key: const Key('campo-refeicao'),
                  readOnly: true,
                  onTap: () => abrirRegistro(context),
                  decoration: const InputDecoration(
                    hintText: 'O que você comeu, ou uma foto',
                    suffixIcon: Icon(Icons.photo_camera_outlined),
                  ),
                ),
                const SizedBox(height: 20),
                const Text('Café · pendente'),
                const Text('Lanche manhã · pendente'),
                const Text('Almoço · pendente'),
                const Text('Lanche tarde · pendente'),
                const Text('Janta · pendente'),
                const Text('Ceia · pendente'),
                const SizedBox(height: 16),
                const Text('Treino hoje · anotar kcal'),
              ],
            );
          },
        ),
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
