import 'package:get_it/get_it.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;

import 'package:nutri/app/router.dart';
import 'package:nutri/cubit/perfil_cubit.dart';
import 'package:nutri/cubit/registro_cubit.dart';
import 'package:nutri/data/estimate_repository.dart';
import 'package:nutri/data/estimate_service.dart';
import 'package:nutri/data/foto_picker.dart';

final getIt = GetIt.instance;

Future<void> setupDependencies({
  http.Client? transport,
  FotoPicker? fotos,
}) async {
  await getIt.reset();
  getIt.registerSingleton<http.Client>(
    transport ?? http.Client(),
    dispose: (client) => client.close(),
  );
  getIt.registerSingleton<FotoPicker>(fotos ?? GaleriaFotoPicker());
  getIt.registerLazySingleton<EstimateService>(
    () => EstimateService(getIt<http.Client>()),
  );
  getIt.registerLazySingleton<EstimateRepository>(
    () => EstimateRepository(getIt<EstimateService>()),
  );
  getIt.registerSingleton<PerfilCubit>(
    PerfilCubit(),
    dispose: (cubit) => cubit.close(),
  );
  getIt.registerFactory<RegistroCubit>(
    () => RegistroCubit(getIt<EstimateRepository>(), getIt<FotoPicker>()),
  );
  getIt.registerSingleton<GoRouter>(
    createRouter(),
    dispose: (router) => router.dispose(),
  );
}
