const url = process.env.NEXT_PUBLIC_BASE_URL;

export const postReview = async (
  method: string,
  review: ReviewRequestDto,
  reviewId: number | null
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/reviews${reviewId ? `/${reviewId}` : ""}`, {
    method: method,
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(review),
  });

  if (!data.ok) {
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
    error.status = data.status;

    throw error;
  }
  return data.json();
};
export const deleteReviewImage = async (
  imageUrls: string[]
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/reviews/images/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ imageUrls }),
  });

  if (!data.ok) {
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
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
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
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
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
    error.status = data.status;

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
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
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
  const data = await fetch(`${url}/reviews/me?page=${page}&size=${size}`, {
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

export const deleteReview = async (id: number): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/reviews/${id}`, {
    method: "DELETE",
    credentials: "include",
  });
  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    let errorMessage = data.statusText;
    try {
      const errorBody = await data.json();
      if (errorBody && errorBody.message) {
        errorMessage = errorBody.message;
      }
    } catch (e) {}

    throw error;
  }
  return data.json();
};
