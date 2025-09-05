const rankingUrl = process.env.NEXT_PUBLIC_RAKUTEN_RANKING;
const searchUrl = process.env.NEXT_PUBLIC_RAKUTEN_SEARCH;
export const rakutenRanking = async (): Promise<RakutenAPIResponse> => {
  const data = await fetch(
    `${rankingUrl}${process.env.NEXT_PUBLIC_RAKUTEN_API_ID}`,
    {
      method: "GET",
      next: { revalidate: 2592000 },
    }
  );
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
export const rakutenSearch = async (
  keyword: string,
  page: number,
  hits: number
): Promise<RakutenAPIResponse> => {
  const data = await fetch(
    `${searchUrl}${
      process.env.NEXT_PUBLIC_RAKUTEN_API_ID
    }&keyword=${keyword.replace(" ", "_")}&page=${page}&hits=${hits}`,
    {
      method: "GET",
      next: { revalidate: 2592000 },
    }
  );
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
