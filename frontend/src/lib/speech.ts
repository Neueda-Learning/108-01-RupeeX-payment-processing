/**
 * Thin wrapper around the browser's Web Speech API — SpeechRecognition for
 * mic-to-text and SpeechSynthesis for text-to-speech. No backend/API keys
 * involved; gracefully no-ops in browsers that don't support it (e.g.
 * Firefox has no SpeechRecognition support at the time of writing).
 */

function getSpeechRecognitionCtor(): { new (): SpeechRecognition } | undefined {
  if (typeof window === "undefined") return undefined;
  return window.SpeechRecognition || window.webkitSpeechRecognition;
}

export function isSpeechRecognitionSupported(): boolean {
  return !!getSpeechRecognitionCtor();
}

export function isSpeechSynthesisSupported(): boolean {
  return typeof window !== "undefined" && "speechSynthesis" in window;
}

export type VoiceListenerHandle = {
  stop: () => void;
};

/**
 * Starts listening on the mic and streams transcripts back via callbacks.
 * Returns a handle to stop listening early, or null if unsupported.
 */
export function startListening(options: {
  onResult: (transcript: string, isFinal: boolean) => void;
  onError?: (message: string) => void;
  onEnd?: () => void;
  lang?: string;
}): VoiceListenerHandle | null {
  const Ctor = getSpeechRecognitionCtor();
  if (!Ctor) {
    options.onError?.("Voice input isn't supported in this browser.");
    return null;
  }

  const recognition = new Ctor();
  recognition.lang = options.lang || "en-IN";
  recognition.continuous = false;
  recognition.interimResults = true;
  recognition.maxAlternatives = 1;

  recognition.onresult = (event: SpeechRecognitionEvent) => {
    let finalTranscript = "";
    let interimTranscript = "";
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const result = event.results[i];
      const transcript = result[0]?.transcript ?? "";
      if (result.isFinal) finalTranscript += transcript;
      else interimTranscript += transcript;
    }
    if (finalTranscript) options.onResult(finalTranscript.trim(), true);
    else if (interimTranscript) options.onResult(interimTranscript.trim(), false);
  };

  recognition.onerror = (event: SpeechRecognitionErrorEvent) => {
    options.onError?.(event.error || "Voice recognition error.");
  };

  recognition.onend = () => {
    options.onEnd?.();
  };

  recognition.start();

  return {
    stop: () => recognition.stop(),
  };
}

/** Reads text aloud using the browser's speech synthesis, if supported. */
export function speak(text: string, lang = "en-IN"): void {
  if (!isSpeechSynthesisSupported() || !text) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = lang;
  window.speechSynthesis.speak(utterance);
}

export function stopSpeaking(): void {
  if (isSpeechSynthesisSupported()) window.speechSynthesis.cancel();
}
