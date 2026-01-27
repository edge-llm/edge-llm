import { requireNativeView } from 'expo';
import * as React from 'react';

import { MediapipeViewProps } from './Mediapipe.types';

const NativeView: React.ComponentType<MediapipeViewProps> =
  requireNativeView('Mediapipe');

export default function MediapipeView(props: MediapipeViewProps) {
  return <NativeView {...props} />;
}
