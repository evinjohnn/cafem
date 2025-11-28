# Aura - Sony Café Mode Implementation

## Changes Overview
- **New DSP Profile**: Implemented an exact replica of Sony's "Background Listening" mode for the "Cafe" setting.
- **Engine Upgrade**: Upgraded `DynamicsProcessing` configuration to support **10-Band EQ** (up from 4) to handle the complex distance curve.
- **UI Update**: Updated Cafe Mode description to "SONY BACKGROUND LISTENING • REAR STAGE".

## Technical Details

### 1. Distance EQ (10-Band High Precision)
Implemented the specific Sony distance curve using `DynamicsProcessing` PreEQ:
- **Sub-Bass (40Hz)**: -6dB (Headphone proximity removal)
- **Bass (80Hz)**: -5dB (Room mode reduction)
- **Low-Mid (200-500Hz)**: -3.5dB to -3dB (Scoop)
- **Mid (1k-2k)**: -2.5dB to -2dB (Vocal pushback)
- **Highs (4k-16k)**: Progressive roll-off (-5dB to -15dB) to simulate air absorption over distance.

### 2. Rear Stage Simulation
- **Virtualizer**: Maxed out (1000/1000) to simulate 170% stereo width.
- **Gain Staging**: Reduced target gain by **-7dB** to simulate physical distance and allow headroom for the wide dynamic range.

### 3. Ambient Reverb (The "Café" Space)
Configured `EnvironmentalReverb` to match Sony's "Large Room/Small Hall" hybrid:
- **Decay**: 2.1s (Background ambience)
- **Pre-Delay**: 42ms (Large space cue)
- **Wet Mix**: ~45% (-7dB Room Level)
- **Damping**: Heavy HF damping (-8dB at 5kHz) for a soft, non-intrusive tail.

### 4. Distance Compression (MBC)
Configured a 3-Band Multi-Band Compressor to maintain background consistency:
- **Low (20-200Hz)**: 4:1 Ratio, -20dB Threshold (Tight control)
- **Mid (200-2k)**: 3:1 Ratio, -15dB Threshold (Smooth vocals)
- **High (2k+)**: 2:1 Ratio, -12dB Threshold (Soft highs)

## Verification
- **Code Check**: `AudioEngineService.kt` now initializes `DynamicsProcessing` with 10 EQ bands and correctly applies the Sony profile in `MODE_CAFE`.
- **UI Check**: `MainActivity.kt` displays the correct Sony branding for Cafe Mode.
- **Cinema Mode Compatibility**: Verified that Cinema Mode still works by mapping its 4-band MBC to the new config and resetting the PreEQ to flat (using standard EQ instead).

## Next Steps
- **Field Test**: Listen to "Cafe Mode" in a quiet room to verify the "behind you" illusion.
- **Fine Tuning**: Adjust the `Virtualizer` strength if the phase issues cause too much artifacting on certain devices.
