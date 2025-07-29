const url = process.env.NEXT_PUBLIC_BASE_URL;

export const postReview = async (
  review: ReviewRequestDto
): Promise<ResponseDTO> => {
  console.log(review);
  const data = await fetch(`${url}/reviews`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(review),
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const getReviewDetail = async (id: number): Promise<Review> => {
  const data = await fetch(`${url}/reviews/${id}`, {
    method: "POST",
    credentials: "include",
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
export const getReviewByProduct = async (
  itemCode: string
): Promise<ReviewListDTO> => {
  const data = await fetch(`${url}/reviews?itemCode=${itemCode}`, {
    method: "GET",
    credentials: "include",
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
