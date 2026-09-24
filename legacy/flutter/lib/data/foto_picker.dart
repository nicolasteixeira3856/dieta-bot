import 'dart:typed_data';

import 'package:image_picker/image_picker.dart';

/// Origem da foto do registro. A tela só pede bytes; quem escolhe é esta classe.
abstract interface class FotoPicker {
  Future<Uint8List?> escolher();
}

class GaleriaFotoPicker implements FotoPicker {
  GaleriaFotoPicker({ImagePicker? picker}) : _picker = picker ?? ImagePicker();

  final ImagePicker _picker;

  @override
  Future<Uint8List?> escolher() async {
    try {
      final arquivo = await _picker.pickImage(source: ImageSource.gallery);
      if (arquivo == null) {
        return null;
      }
      return arquivo.readAsBytes();
    } catch (_) {
      return null;
    }
  }
}
