import { requireNativeModule } from "expo-modules-core";

const Mediapipe = requireNativeModule("Mediapipe");

export * from "./memory";
export * from "./Mediapipe.types";
export { default as MediapipeView } from "./MediapipeView";

export default Mediapipe;
