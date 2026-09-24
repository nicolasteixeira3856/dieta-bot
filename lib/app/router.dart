import 'package:go_router/go_router.dart';

import 'package:nutri/view/home_page.dart';
import 'package:nutri/view/onboarding_teto_page.dart';
import 'package:nutri/view/onboarding_treino_page.dart';

GoRouter createRouter() {
  return GoRouter(
    initialLocation: '/onboarding/teto',
    routes: [
      GoRoute(
        path: '/onboarding/teto',
        builder: (context, state) => const OnboardingTetoPage(),
      ),
      GoRoute(
        path: '/onboarding/treino',
        builder: (context, state) => const OnboardingTreinoPage(),
      ),
      GoRoute(path: '/', builder: (context, state) => const HomePage()),
    ],
  );
}
