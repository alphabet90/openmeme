"use client";

import { useEffect, useState } from "react";
import { MemeListingGrid, type Meme } from "@openmeme/ui";

type Props = {
  memes: Meme[];
  ariaLabel: string;
};

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export function ShuffledMemeGrid({ memes, ariaLabel }: Props) {
  const [shuffled, setShuffled] = useState<Meme[] | null>(null);

  useEffect(() => {
    setShuffled(shuffle(memes));
  }, [memes]);

  if (!shuffled) {
    return <MemeListingGrid memes={memes} ariaLabel={ariaLabel} priorityCount={5} />;
  }

  return <MemeListingGrid memes={shuffled} ariaLabel={ariaLabel} priorityCount={5} />;
}
