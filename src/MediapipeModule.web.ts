import { registerWebModule, NativeModule } from 'expo';

import { ChangeEventPayload } from './Mediapipe.types';

type MediapipeModuleEvents = {
  onChange: (params: ChangeEventPayload) => void;
}

class MediapipeModule extends NativeModule<MediapipeModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
};

export default registerWebModule(MediapipeModule, 'MediapipeModule');
