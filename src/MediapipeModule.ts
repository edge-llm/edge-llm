import { NativeModule, requireNativeModule } from 'expo';

import { MediapipeModuleEvents } from './Mediapipe.types';

declare class MediapipeModule extends NativeModule<MediapipeModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<MediapipeModule>('Mediapipe');
