"use client";

import { useRef, useState } from "react";
import CustomButton from "../common/CustomBtn";
import { postReview, postUploadImage } from "@/api/Review";
import { useRouter } from "next/navigation";
import CustomInput from "../common/CustomInput";
import UserStore from "@/store/user";
import { IoCameraOutline, IoCloseSharp } from "react-icons/io5";
interface props {
  itemCode: string;
}
export default function ReviewWriteContain({ itemCode }: props) {
  const [hovered, setHovered] = useState(0);
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState("");
  const [title, setTitle] = useState("");
  const router = useRouter();
  const userStore = UserStore();
  const inputRef = useRef<HTMLInputElement>(null);
  const [imageList, setImageList] = useState<File[]>([]);
  const [uploadedImages, setUploadedImages] = useState<string[]>([]);
  const onImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;

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
    if (!rating) return alert("レビューには星の評価が必要です。");
    if (!content) return alert("レビュー内容を入力してください。");
    if (imageList && imageList.length > 0) {
      const imageUrls = await postUploadImage(imageList);
      setUploadedImages(imageUrls.imageUrls);
    }
    const review: ReviewRequestDto = {
      itemCode: decodeURIComponent(itemCode),
      rating,
      comment: content,
      title: title,
      imageList: uploadedImages,
    };
    try {
      const res = await postReview(review);
      if (res.message === "success") {
        alert("レビューが投稿されました。");
        router.back();
      } else {
        alert("レビューの投稿に失敗しました。");
      }
    } catch (e: any) {
      console.log(e);
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        router.push("/login");
      } else {
        alert("レビューの投稿中にエラーが発生しました。");
      }
    }
  };
  return (
    <div className="mt-2">
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
        <div className="wf flex je ">
          <CustomButton
            func={onPostReview}
            text="投稿"
            classname="review-post-btn"
          />
        </div>
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
    </div>
  );
}
