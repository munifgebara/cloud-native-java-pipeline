export const IMAGE_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'] as const;

export type ClipboardImageResult =
  | { ok: true; file: File }
  | { ok: false; reason: 'missing' | 'invalid-type' | 'too-large' };

export function imageFileFromPaste(event: ClipboardEvent, maxSizeBytes: number): ClipboardImageResult {
  const file = firstClipboardFile(event);

  if (!file) {
    return { ok: false, reason: 'missing' };
  }

  if (!IMAGE_CONTENT_TYPES.includes(file.type as (typeof IMAGE_CONTENT_TYPES)[number])) {
    return { ok: false, reason: 'invalid-type' };
  }

  if (file.size > maxSizeBytes) {
    return { ok: false, reason: 'too-large' };
  }

  event.preventDefault();
  return { ok: true, file };
}

function firstClipboardFile(event: ClipboardEvent): File | null {
  const files = Array.from(event.clipboardData?.files ?? []);
  const file = files.find((item) => item.type.startsWith('image/'));

  if (file) {
    return file;
  }

  const items = Array.from(event.clipboardData?.items ?? []);
  return items.find((item) => item.kind === 'file' && item.type.startsWith('image/'))?.getAsFile() ?? null;
}
