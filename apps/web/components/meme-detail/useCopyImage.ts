"use client";

import { useState, useRef, useCallback, useEffect } from "react";

export type CopyState = "idle" | "copied" | "error";

export interface UseCopyImageResult {
  state: CopyState;
  copy: () => Promise<void>;
}

function blobToPng(blob: Blob): Promise<Blob> {
  if (blob.type === "image/png") {
    return Promise.resolve(blob);
  }
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(blob);
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = img.naturalWidth;
      canvas.height = img.naturalHeight;
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        URL.revokeObjectURL(url);
        reject(new Error("Canvas context unavailable"));
        return;
      }
      ctx.drawImage(img, 0, 0);
      canvas.toBlob(
        (pngBlob) => {
          URL.revokeObjectURL(url);
          if (pngBlob) {
            resolve(pngBlob);
          } else {
            reject(new Error("Canvas toBlob returned null"));
          }
        },
        "image/png",
      );
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("Image load failed"));
    };
    img.src = url;
  });
}

function imageUrlToPngBlob(imageUrl: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = img.naturalWidth;
      canvas.height = img.naturalHeight;
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        reject(new Error("Canvas context unavailable"));
        return;
      }
      ctx.drawImage(img, 0, 0);
      canvas.toBlob(
        (pngBlob) => {
          if (pngBlob) {
            resolve(pngBlob);
          } else {
            reject(new Error("Canvas toBlob returned null"));
          }
        },
        "image/png",
      );
    };
    img.onerror = () => reject(new Error("Image load failed"));
    img.src = imageUrl;
  });
}

async function writeImageToClipboard(pngBlob: Blob): Promise<void> {
  if (!navigator.clipboard || !navigator.clipboard.write) {
    throw new Error("Clipboard API unavailable");
  }
  await navigator.clipboard.write([
    new ClipboardItem({ "image/png": pngBlob }),
  ]);
}

export function useCopyImage(imageUrl: string): UseCopyImageResult {
  const [state, setState] = useState<CopyState>("idle");
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  const scheduleReset = useCallback((nextState: CopyState) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    timeoutRef.current = setTimeout(() => {
      if (mountedRef.current) {
        setState("idle");
      }
    }, 2000);
    setState(nextState);
  }, []);

  const copy = useCallback(async () => {
    if (!navigator.clipboard) {
      scheduleReset("error");
      return;
    }

    try {
      // 1. Try fetch with CORS
      const response = await fetch(imageUrl, { mode: "cors" });
      if (response.ok) {
        const blob = await response.blob();
        const pngBlob = await blobToPng(blob);
        await writeImageToClipboard(pngBlob);
        scheduleReset("copied");
        return;
      }
    } catch {
      // fetch failed — try canvas fallback
    }

    try {
      // 2. Canvas fallback with crossOrigin image
      const pngBlob = await imageUrlToPngBlob(imageUrl);
      await writeImageToClipboard(pngBlob);
      scheduleReset("copied");
      return;
    } catch {
      // canvas approach failed — fall back to text
    }

    try {
      // 3. Fallback to copying the URL as text
      await navigator.clipboard.writeText(imageUrl);
      scheduleReset("copied");
    } catch {
      scheduleReset("error");
    }
  }, [imageUrl, scheduleReset]);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return { state, copy };
}
