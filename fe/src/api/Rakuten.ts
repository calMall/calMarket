import { RakutenAPIResponse } from "@/types/RakutenAPI";

const rankingUrl = process.env.NEXT_PUBLIC_RAKUTEN_RANKING;

export const rakutenRanking = async (): Promise<RakutenAPIResponse> => {
  const data = await fetch(
    `${rankingUrl}${process.env.NEXT_PUBLIC_RAKUTEN_API_ID}`,
    {
      method: "GET",
    }
  );
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
