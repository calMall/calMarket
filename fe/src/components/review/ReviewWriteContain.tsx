"use client";

import { useEffect, useRef, useState } from "react";
import CustomButton from "../common/CustomBtn";
import { deleteReviewImage, postReview, postUploadImage } from "@/api/Review";
import { useRouter } from "next/navigation";
import CustomInput from "../common/CustomInput";
import UserStore from "@/store/user";
import { IoCameraOutline, IoCloseSharp } from "react-icons/io5";
import { FaCheck } from "react-icons/fa";
import CustomAlert from "../common/CustomAlert";
interface props {
  itemCode: string;
  initialData: ReviewDTOonProduct | null;
  reviewId?: number;
}
export default function ReviewWriteContain({
  itemCode,
  initialData,
  reviewId,
}: props) {
  const [hovered, setHovered] = useState(initialData ? initialData.rating : 0);
  const [rating, setRating] = useState(initialData ? initialData.rating : 0);
  const [content, setContent] = useState(
    initialData ? initialData.comment : ""
  );
  const [title, setTitle] = useState(initialData ? initialData.title : "");
  const router = useRouter();
  const userStore = UserStore();
  const inputRef = useRef<HTMLInputElement>(null);
  const [imageList, setImageList] = useState<File[]>([]);
  const [initialImages, setInitialImages] = useState<string[]>(
    initialData ? initialData.imageList : []
  );
  const [selectedImage, setSelectedImage] = useState<string[]>([]);
  const [isImageUploading, setIsImageUploading] = useState(false);
  const [isReviewloading, setIsReviewUploading] = useState(false);
  const deleteInitImage = async () => {
    if (window.confirm("選択した画像を削除しますか？")) {
      if (selectedImage.length === 0)
        return alert("選択された画像がありません");
      try {
        const data = await deleteReviewImage(selectedImage);
        if (data.message === "success") {
          setInitialImages((prev) => {
            return prev.filter((p) => !selectedImage.includes(p));
          });
          setSelectedImage([]);
          alert();
        }
      } catch (e: any) {
        if (e.status === 401) {
          alert("ログインが必要です。ログインページに移動します。");
          userStore.logout();
          router.push("/login");
        } else {
          alert(e.message);
        }
      }
    }
  };
  const clickInitImage = (name: string) => {
    if (selectedImage.includes(name)) {
      setSelectedImage((prev) => prev.filter((img) => name !== img));
    } else {
      setSelectedImage((prev) => [...prev, name]);
    }
  };
  const onImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (initialImages) {
      if (files && initialImages.length + files.length > 3) {
        alert("画像は3枚までアップロードできます。");
        return;
      }
    }
    if (files && files.length > 3) {
      alert("画像は3枚までアップロードできます。");
      return;
    }
    if (files) {
      const newImages = Array.from(files);
      setImageList((prev) => [...prev, ...newImages]);
    }
    console.log(imageList);
  };

  const changeContent = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (e.target.value.length > 500) {
      return alert("500文字以内で入力してください。");
    } else {
      setContent(e.target.value);
    }
  };
  const deleteImage = (imageName: string) => {
    setImageList((prev) => prev.filter((image) => image.name !== imageName));
  };

  const onPostReview = async () => {
    const method = initialData ? "PATCH" : "POST";
    if (!rating) return alert("レビューには星の評価が必要です。");
    if (!content) return alert("レビュー内容を入力してください。");
    try {
      let uploaded: string[] = [];

      if (imageList && imageList.length > 0) {
        setIsImageUploading(true);
        const imageUrls = await postUploadImage(imageList);
        uploaded = imageUrls.imageUrls;
        setIsImageUploading(false);
      }
      if (initialImages.length > 0) {
        uploaded = [...uploaded, ...initialImages];
      }
      const review: ReviewRequestDto = {
        ...(initialData ? {} : { itemCode: decodeURIComponent(itemCode) }),
        rating,
        comment: content,
        title: title,
        imageList: uploaded,
      };
      setIsReviewUploading(true);
      const res = await postReview(
        method,
        review,
        reviewId && initialData ? reviewId : null
      );
      console.log(res);
      setIsReviewUploading(false);

      if (method === "POST") {
        alert("レビューが投稿されました。");
      } else if (method === "PATCH") {
        alert("レビューを編集しました。");
      }
      router.back();
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        router.push("/login");
      } else {
        if (method === "POST") {
          return alert("レビューの投稿に失敗しました。");
        } else if (method === "PATCH") {
          return alert("レビューの編集に失敗しました。");
        }
        alert(e.message);
      }
    }
  };

  return (
    <div className="mt-2">
      {(isImageUploading || isReviewloading) && (
        <CustomAlert
          status="loading"
          text={
            isImageUploading
              ? "画像をアップロードしています"
              : "レビューを投稿しています"
          }
        />
      )}
      <div style={{ display: "flex", gap: "4px" }}>
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            style={{
              cursor: "pointer",
              fontSize: "2.5rem",
              color: (hovered || rating) >= star ? "#ffc107" : "#e4e5e9",
            }}
            onMouseEnter={() => setHovered(star)}
            onMouseLeave={() => setHovered(0)}
            onClick={() => setRating(star)}
          >
            ★
          </span>
        ))}
      </div>
      <div className="flex flex-col gap-1 mt-1">
        <h4>タイトル</h4>
        <CustomInput
          text={title}
          setText={setTitle}
          placeholder="タイトルを入力してください"
        />
        <h4>レビューを書く</h4>
        <div>
          <textarea
            placeholder="他のお客様が知っておくべきことは何ですか？(最大５００文字)"
            className="wf pd-1 border cart-border bx"
            value={content}
            onChange={changeContent}
          />
        </div>
        {initialImages.length > 0 && (
          <>
            <div className="flex jb ac">
              <h4>登録されている画像</h4>
              <button className="cart-del-btn" onClick={deleteInitImage}>
                選択した画像を削除
              </button>
            </div>
            <div className="image-upload-btn bx flex ac jc gap-1">
              {initialImages &&
                initialImages.length > 0 &&
                initialImages.map((image) => (
                  <button
                    key={image}
                    onClick={() => clickInitImage(image)}
                    className="rt image-preview hf flex ac jc hover-anime"
                  >
                    {selectedImage.includes(image) && (
                      <div className="selected-img flex ac jc ab">
                        <FaCheck />
                      </div>
                    )}
                    <img src={image} alt="review-img" />
                  </button>
                ))}
            </div>
          </>
        )}

        <h4>画像をアップロード</h4>
        <div className="image-upload-btn bx flex ac jc gap-1">
          {imageList.length <= 0 ? (
            <button
              className="wf hf flex ac jc font-xx-large"
              onClick={() => inputRef.current?.click()}
              disabled={imageList.length > 0}
            >
              <IoCameraOutline />
            </button>
          ) : (
            imageList.map((image) => (
              <div key={image.name} className="rt image-preview hf flex ac jc">
                <img src={URL.createObjectURL(image)} alt={image.name} />
                <button
                  onClick={() => deleteImage(image.name)}
                  className="delete-img-btn ab flex ac jc"
                >
                  <IoCloseSharp className="flex ac jc" />
                </button>
              </div>
            ))
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          onChange={onImageChange}
          hidden
          multiple
          accept="image/*"
        />
      </div>
      <div className="wf flex je mt-1">
        <CustomButton
          func={onPostReview}
          text={initialData ? "編集" : "投稿"}
          classname="review-post-btn"
        />
      </div>
    </div>
  );
}
