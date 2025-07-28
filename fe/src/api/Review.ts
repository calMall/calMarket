const url = process.env.NEXT_PUBLIC_BASE_URL;

export const postReview = async (
  review: ReviewRequestDto
): Promise<ResponseDTO> => {
  console.log(review);
  const data = await fetch(`${url}/reviews`, {
    method: "POST",
    credentials: "include",
    body: JSON.stringify(review),
  });

  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
