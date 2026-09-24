import 'package:flutter/material.dart';

const nutriBg = Color(0xFF0B0D10);
const nutriSurface = Color(0xFF171B20);
const nutriSurf2 = Color(0xFF1E242B);
const nutriLine = Color(0xFF2A3139);
const nutriText = Color(0xFFF3F5F7);
const nutriMuted = Color(0xFF8B939C);
const nutriDim = Color(0xFF5C6570);
const nutriGold = Color(0xFFE8B86D);
const nutriGoldDim = Color(0x24E8B86D);
const nutriOk = Color(0xFF7DDA9A);
const nutriError = Color(0xFFE07A6A);
const nutriInk = Color(0xFF111111);

const nutriMotion = Duration(milliseconds: 200);

class NutriTokens extends ThemeExtension<NutriTokens> {
  const NutriTokens({
    required this.muted,
    required this.dim,
    required this.goldDim,
    required this.ok,
  });

  final Color muted;
  final Color dim;
  final Color goldDim;
  final Color ok;

  @override
  NutriTokens copyWith({
    Color? muted,
    Color? dim,
    Color? goldDim,
    Color? ok,
  }) {
    return NutriTokens(
      muted: muted ?? this.muted,
      dim: dim ?? this.dim,
      goldDim: goldDim ?? this.goldDim,
      ok: ok ?? this.ok,
    );
  }

  @override
  NutriTokens lerp(ThemeExtension<NutriTokens>? other, double t) {
    if (other is! NutriTokens) {
      return this;
    }
    return NutriTokens(
      muted: Color.lerp(muted, other.muted, t)!,
      dim: Color.lerp(dim, other.dim, t)!,
      goldDim: Color.lerp(goldDim, other.goldDim, t)!,
      ok: Color.lerp(ok, other.ok, t)!,
    );
  }
}

ThemeData nutriTheme() {
  const scheme = ColorScheme.dark(
    surface: nutriSurface,
    primary: nutriGold,
    onPrimary: nutriInk,
    secondary: nutriOk,
    onSecondary: nutriInk,
    error: nutriError,
    onError: nutriInk,
    onSurface: nutriText,
    outline: nutriLine,
    outlineVariant: nutriLine,
  );
  final base = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: nutriBg,
    canvasColor: nutriBg,
    colorScheme: scheme,
  );
  final text = base.textTheme.apply(
    bodyColor: nutriText,
    displayColor: nutriText,
  );
  final fieldBorder = OutlineInputBorder(
    borderRadius: BorderRadius.circular(16),
    borderSide: const BorderSide(color: nutriLine),
  );
  return base.copyWith(
    scaffoldBackgroundColor: nutriBg,
    textTheme: text.copyWith(
      headlineMedium: text.headlineMedium?.copyWith(
        fontSize: 28,
        height: 1.15,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.6,
        color: nutriText,
      ),
      titleLarge: text.titleLarge?.copyWith(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: nutriText,
      ),
      bodyMedium: text.bodyMedium?.copyWith(
        fontSize: 14,
        height: 1.45,
        color: nutriText,
      ),
      bodySmall: text.bodySmall?.copyWith(
        fontSize: 13,
        height: 1.4,
        color: nutriMuted,
      ),
    ),
    iconTheme: const IconThemeData(color: nutriMuted),
    extensions: const [
      NutriTokens(
        muted: nutriMuted,
        dim: nutriDim,
        goldDim: nutriGoldDim,
        ok: nutriOk,
      ),
    ],
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: nutriText,
        foregroundColor: nutriInk,
        disabledBackgroundColor: nutriLine,
        disabledForegroundColor: nutriMuted,
        minimumSize: const Size(64, 52),
        elevation: 0,
        shape: const StadiumBorder(),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: nutriText,
        minimumSize: const Size(0, 44),
        side: const BorderSide(color: nutriLine),
        shape: const StadiumBorder(),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: nutriSurface,
      hintStyle: const TextStyle(color: nutriDim, fontSize: 14),
      labelStyle: const TextStyle(color: nutriMuted, fontSize: 12),
      suffixStyle: const TextStyle(
        color: nutriMuted,
        fontSize: 14,
        fontWeight: FontWeight.w500,
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: fieldBorder,
      enabledBorder: fieldBorder,
      focusedBorder: fieldBorder.copyWith(
        borderSide: const BorderSide(color: nutriGold),
      ),
    ),
    cardTheme: const CardThemeData(
      color: nutriSurface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(18)),
        side: BorderSide(color: nutriLine),
      ),
    ),
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: nutriSurface,
      modalBackgroundColor: nutriSurface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
      ),
    ),
    textSelectionTheme: const TextSelectionThemeData(
      cursorColor: nutriGold,
      selectionColor: nutriGoldDim,
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(color: nutriGold),
  );
}

/// Phone column from the wire. Wide windows stay one column, centered.
class NutriColumn extends StatelessWidget {
  const NutriColumn({super.key, required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 430),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
          children: children,
        ),
      ),
    );
  }
}

class NutriOption extends StatelessWidget {
  const NutriOption({
    super.key,
    required this.selected,
    required this.onTap,
    required this.child,
  });

  final bool selected;
  final VoidCallback onTap;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tokens = theme.extension<NutriTokens>()!;
    return AnimatedContainer(
      duration: nutriMotion,
      curve: Curves.easeOutCubic,
      width: double.infinity,
      decoration: BoxDecoration(
        color: selected ? tokens.goldDim : theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: selected
              ? theme.colorScheme.primary
              : theme.colorScheme.outline,
        ),
      ),
      child: Material(
        type: MaterialType.transparency,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(18),
          child: Padding(padding: const EdgeInsets.all(14), child: child),
        ),
      ),
    );
  }
}

/// Press scales to 0.97 from the resting scale of 1.
class NutriPress extends StatefulWidget {
  const NutriPress({super.key, required this.child});

  final Widget child;

  @override
  State<NutriPress> createState() => _NutriPressState();
}

class _NutriPressState extends State<NutriPress> {
  var _down = false;

  @override
  Widget build(BuildContext context) {
    return Listener(
      onPointerDown: (_) => setState(() => _down = true),
      onPointerUp: (_) => setState(() => _down = false),
      onPointerCancel: (_) => setState(() => _down = false),
      child: AnimatedScale(
        scale: _down ? 0.97 : 1,
        duration: nutriMotion,
        curve: Curves.easeOutCubic,
        child: widget.child,
      ),
    );
  }
}

Widget nutriCta(String label, VoidCallback? onPressed) {
  return NutriPress(
    child: FilledButton(onPressed: onPressed, child: Text(label)),
  );
}

Text nutriKicker(String text) {
  return Text(
    text,
    style: const TextStyle(
      fontSize: 11,
      letterSpacing: 1.3,
      fontWeight: FontWeight.w600,
      color: nutriDim,
    ),
  );
}
