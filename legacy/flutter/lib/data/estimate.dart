/// Resposta de POST /v1/estimate. [pergunta] pode vir no JSON e ainda
/// ficar de fora da tela quando a confiança é alta.
class Estimate {
  const Estimate({
    required this.kcal,
    required this.p,
    required this.c,
    required this.g,
    required this.confianca,
    required this.pergunta,
    required this.model,
  });

  factory Estimate.fromJson(Map<String, dynamic> json) {
    return Estimate(
      kcal: _numero(json['kcal']),
      p: _numero(json['p']),
      c: _numero(json['c']),
      g: _numero(json['g']),
      confianca: json['confianca'] as String? ?? 'baixa',
      pergunta: json['pergunta'] as String?,
      model: json['model'] as String? ?? '',
    );
  }

  final num kcal;
  final num p;
  final num c;
  final num g;
  final String confianca;
  final String? pergunta;
  final String model;

  bool get mostraPergunta {
    final texto = pergunta?.trim() ?? '';
    return confianca.trim().toLowerCase() != 'alto' && texto.isNotEmpty;
  }
}

num _numero(Object? valor) {
  if (valor is num) {
    return valor;
  }
  throw FormatException('numero ausente');
}
