import * as React from 'react';

import { MediapipeViewProps } from './Mediapipe.types';

export default function MediapipeView(props: MediapipeViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
