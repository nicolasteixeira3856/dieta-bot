import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'package:nutri/app/dependencies.dart';
import 'package:nutri/app/nutri_theme.dart';
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
        theme: nutriTheme(),
        routerConfig: getIt<GoRouter>(),
      ),
    );
  }
}
