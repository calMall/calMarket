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
export const postReviewLike = async (
  reviewId: number
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/review-likes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ reviewId }),
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const getReviewDetail = async (
  id: number
): Promise<ReviewDTOonProduct> => {
  const data = await fetch(`${url}/reviews/${id}`, {
    method: "POST",
    credentials: "include",
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    console.log(error);
    throw error;
  }
  return data.json();
};
export const getReviewByProduct = async (
  itemCode: string,
  page: number,
  size: number
): Promise<ReviewListDTO> => {
  const data = await fetch(
    `${url}/reviews?itemCode=${itemCode}&page=${page}&size=${size}`,
    {
      method: "GET",
      credentials: "include",
    }
  );

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const postUploadImage = async (
  files: File[]
): Promise<ImageUploadDto> => {
  const formData = new FormData();

  files.forEach((file) => {
    formData.append("files", file);
  });

  const data = await fetch(`${url}/reviews/images/upload`, {
    method: "POST",
    credentials: "include",
    body: formData,
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const getReviewByUser = async (
  page: number,
  size: number
): Promise<ReviewListDTO> => {
  const data = await fetch(`${url}/reviews`, {
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
