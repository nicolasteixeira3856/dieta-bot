import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'package:nutri/app/dependencies.dart';
import 'package:nutri/cubit/perfil_cubit.dart';

class NutriApp extends StatelessWidget {
  const NutriApp({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider<PerfilCubit>.value(
      value: getIt<PerfilCubit>(),
      child: MaterialApp.router(
        title: 'Nutri',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1F6B4A)),
          useMaterial3: true,
        ),
        routerConfig: getIt<GoRouter>(),
      ),
    );
  }
}
